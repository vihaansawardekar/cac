import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import javax.imageio.ImageIO;

// shows our two equations
// NOT used by the app
// run: java SoilEquations.java
public class SoilEquations {

    // EQUATION 1: soil organic matter next year
    // OM(t+1) = OM(t) + lambda * [ g * (1 - 0.2 * delta) + m - k * OM(t) ] + (1 - lambda) * beta
    public static double nextOrganicMatter(double om, double g, int delta, double m, double k,
                                           double lambda, double beta) {
        double gain = g * (1 - 0.2 * delta);
        double model = gain + m - k * om;
        return om + lambda * model + (1 - lambda) * beta;
    }

    // k: how fast soil breaks down
    public static double decayRate(double tillage, double climate) {
        return 0.02 * tillage * climate;
    }

    // EQUATION 2: one convolution filter
    // h[r][c] = relu( b + sum of K[i][j][ch] * X[r+i][c+j][ch] )
    static float[][] convFilter(float[][][] X, float[][][] K, float b) {
        int size = X.length - 2;
        float[][] h = new float[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                float sum = b;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        for (int ch = 0; ch < X[0][0].length; ch++) {
                            sum = sum + K[i][j][ch] * X[r + i][c + j][ch];
                        }
                    }
                }
                h[r][c] = Math.max(0, sum);
            }
        }
        return h;
    }

    // whole soil photo AI
    // uses equation 2 with 16 + 32 + 32 filters
    public static double predictOrganicMatter(float[][][] X) {
        float[][][] h1 = maxPool(layer(X, "K1", "b1"));
        float[][][] h2 = maxPool(layer(h1, "K2", "b2"));
        float[] h3 = averagePool(layer(h2, "K3", "b3"));
        float[] h4 = relu(dense(h3, w.get("W1"), w.get("c1")));
        float[] om = dense(h4, w.get("w2"), w.get("c2"));
        return om[0];
    }

    // trained numbers from the app model
    static HashMap<String, float[]> w = new HashMap<>();

    // one layer of filters
    static float[][][] layer(float[][][] X, String kName, String bName) {
        float[] k = w.get(kName);
        float[] b = w.get(bName);
        int inC = X[0][0].length;
        int size = X.length - 2;
        float[][][] out = new float[size][size][b.length];
        for (int o = 0; o < b.length; o++) {
            // pull out filter o
            float[][][] K = new float[3][3][inC];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int ch = 0; ch < inC; ch++) {
                        K[i][j][ch] = k[((o * 3 + i) * 3 + j) * inC + ch];
                    }
                }
            }
            float[][] h = convFilter(X, K, b[o]);
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    out[r][c][o] = h[r][c];
                }
            }
        }
        return out;
    }

    // sigma: keep positive numbers
    static float[] relu(float[] x) {
        for (int i = 0; i < x.length; i++) {
            x[i] = Math.max(0, x[i]);
        }
        return x;
    }

    // P: 2 by 2 max pool
    static float[][][] maxPool(float[][][] x) {
        int size = x.length / 2;
        int channels = x[0][0].length;
        float[][][] out = new float[size][size][channels];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                for (int ch = 0; ch < channels; ch++) {
                    float a = Math.max(x[2 * r][2 * c][ch], x[2 * r][2 * c + 1][ch]);
                    float b = Math.max(x[2 * r + 1][2 * c][ch], x[2 * r + 1][2 * c + 1][ch]);
                    out[r][c][ch] = Math.max(a, b);
                }
            }
        }
        return out;
    }

    // GAP: average each channel
    static float[] averagePool(float[][][] x) {
        int channels = x[0][0].length;
        float[] out = new float[channels];
        for (int r = 0; r < x.length; r++) {
            for (int c = 0; c < x[0].length; c++) {
                for (int ch = 0; ch < channels; ch++) {
                    out[ch] = out[ch] + x[r][c][ch];
                }
            }
        }
        for (int ch = 0; ch < channels; ch++) {
            out[ch] = out[ch] / (x.length * x[0].length);
        }
        return out;
    }

    // W * h + c
    static float[] dense(float[] h, float[] weights, float[] bias) {
        float[] out = new float[bias.length];
        for (int o = 0; o < bias.length; o++) {
            float sum = bias[o];
            for (int i = 0; i < h.length; i++) {
                sum = sum + weights[o * h.length + i] * h[i];
            }
            out[o] = sum;
        }
        return out;
    }

    // read the weights file
    static void loadWeights(String path) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String header = reader.readLine();
        while (header != null) {
            String[] parts = header.split(" ");
            String[] numbers = reader.readLine().trim().split(" ");
            float[] values = new float[numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                values[i] = Float.parseFloat(numbers[i]);
            }
            w.put(parts[0], values);
            header = reader.readLine();
        }
        reader.close();
    }

    // photo to 64 by 64 numbers
    static float[][][] readPhoto(String path) throws Exception {
        BufferedImage photo = ImageIO.read(new File(path));
        int side = Math.min(photo.getWidth(), photo.getHeight());
        BufferedImage square = photo.getSubimage((photo.getWidth() - side) / 2, (photo.getHeight() - side) / 2, side, side);
        BufferedImage small = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = small.createGraphics();
        pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        pen.drawImage(square, 0, 0, 64, 64, null);
        pen.dispose();

        float[][][] x = new float[64][64][3];
        for (int r = 0; r < 64; r++) {
            for (int c = 0; c < 64; c++) {
                int pixel = small.getRGB(c, r);
                x[r][c][0] = ((pixel >> 16) & 255) / 255.0f;
                x[r][c][1] = ((pixel >> 8) & 255) / 255.0f;
                x[r][c][2] = (pixel & 255) / 255.0f;
            }
        }
        return x;
    }

    public static void main(String[] args) throws Exception {
        // equation 1 test: 3 years of corn, no-till, cool and wet
        System.out.println("Equation 1: organic matter each year");
        double om = 1.33;
        double k = decayRate(0.75, 0.85);
        for (int year = 1; year <= 3; year++) {
            om = nextOrganicMatter(om, 0.07, 1, 0, k, 1.0, 0);
            System.out.printf("  Year %d: %.2f%%%n", year, om);
        }

        // equation 2 test: soil photos
        System.out.println("Equation 2: soil photo AI");
        loadWeights("soil_model_weights.txt");
        String[] photos = {"soil_om_1.0.png", "soil_om_3.0.png", "soil_om_6.5.png"};
        for (int i = 0; i < photos.length; i++) {
            float[][][] x = readPhoto("../ml/test_photos/" + photos[i]);
            System.out.printf("  %s: %.2f%%%n", photos[i], predictOrganicMatter(x));
        }
    }
}
