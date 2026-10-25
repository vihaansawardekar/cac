# trains the soil AI
# see README for help

import os
import math
import random
import numpy as np
import tensorflow as tf
from PIL import Image

SIZE = 64
HERE = os.path.dirname(os.path.abspath(__file__))
REAL_FOLDER = os.path.join(HERE, "real_photos")
TEST_FOLDER = os.path.join(HERE, "test_photos")
MODEL_FILE = os.path.join(HERE, "..", "app", "src", "main", "assets", "soil_model.tflite")

random.seed(7)
np.random.seed(7)
tf.random.set_seed(7)


# helper for color math
def lab_helper(t):
    if t ** 3 > 0.008856:
        return t ** 3
    return (t - 16 / 116) / 7.787


# lab color to rgb
def lab_to_rgb(L, a, b):
    y = (L + 16) / 116
    x = a / 500 + y
    z = y - b / 200
    X = 0.95047 * lab_helper(x)
    Y = 1.00000 * lab_helper(y)
    Z = 1.08883 * lab_helper(z)
    r = X * 3.2406 + Y * -1.5372 + Z * -0.4986
    g = X * -0.9689 + Y * 1.8758 + Z * 0.0415
    bl = X * 0.0557 + Y * -0.2040 + Z * 1.0570

    out = []
    for c in [r, g, bl]:
        c = max(c, 0)
        if c > 0.0031308:
            c = 1.055 * (c ** (1 / 2.4)) - 0.055
        else:
            c = 12.92 * c
        out.append(min(max(c, 0), 1))
    return np.array(out)


# make one practice photo
def make_fake_photo(om, size):
    # darker soil, more organic
    L = 50.5 - 13.7 * math.log(om) + random.uniform(-3, 3)
    chroma = max(22 - 2.2 * om + random.uniform(-3, 3), 2)
    hue = math.radians(random.uniform(55, 80))
    color = lab_to_rgb(L, chroma * math.cos(hue), chroma * math.sin(hue))

    # change the sunlight
    color = color * random.uniform(0.9, 1.1)
    img = np.ones((size, size, 3)) * color

    # add soil clumps
    clumps = np.random.normal(0, 1, (8, 8)).astype(np.float32)
    clumps = np.array(Image.fromarray(clumps).resize((size, size), Image.BILINEAR))
    img = img * (1 + 0.08 * clumps[:, :, None])

    # add grainy look
    img = img + np.random.normal(0, 0.03 * random.uniform(0.5, 1.5), (size, size, 3))

    # little sand and roots
    for i in range(random.randint(0, 25)):
        x = random.randint(0, size - 2)
        y = random.randint(0, size - 2)
        spot = max(1, size // 64)
        if random.random() < 0.5:
            img[y:y + spot, x:x + spot] = img[y:y + spot, x:x + spot] * 1.4
        else:
            img[y:y + spot, x:x + spot] = img[y:y + spot, x:x + spot] * 0.6

    # shadow across photo
    shade = np.linspace(random.uniform(0.85, 1.0), random.uniform(0.85, 1.0), size)
    if random.random() < 0.5:
        img = img * shade[None, :, None]
    else:
        img = img * shade[:, None, None]

    return np.clip(img, 0, 1).astype(np.float32)


# load real lab photos
def load_real_photos():
    pictures = []
    answers = []
    label_file = os.path.join(REAL_FOLDER, "labels.csv")
    for line in open(label_file):
        parts = line.strip().split(",")
        if len(parts) != 2:
            continue
        try:
            om = float(parts[1])
        except ValueError:
            continue
        photo = Image.open(os.path.join(REAL_FOLDER, parts[0])).convert("RGB")
        side = min(photo.size)
        left = (photo.size[0] - side) // 2
        top = (photo.size[1] - side) // 2
        photo = photo.crop((left, top, left + side, top + side)).resize((SIZE, SIZE))
        pictures.append(np.array(photo, dtype=np.float32) / 255.0)
        answers.append(om)
    return np.array(pictures), np.array(answers, dtype=np.float32)


# make practice photo set
def make_practice_photos(count):
    pictures = []
    answers = []
    for i in range(count):
        om = math.exp(random.uniform(math.log(0.5), math.log(8)))
        pictures.append(make_fake_photo(om, SIZE))
        answers.append(om)
    return np.array(pictures), np.array(answers, dtype=np.float32)


# build the CNN
def make_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(SIZE, SIZE, 3)),
        tf.keras.layers.Conv2D(16, 3, activation="relu"),
        tf.keras.layers.MaxPooling2D(),
        tf.keras.layers.Conv2D(32, 3, activation="relu"),
        tf.keras.layers.MaxPooling2D(),
        tf.keras.layers.Conv2D(32, 3, activation="relu"),
        tf.keras.layers.GlobalAveragePooling2D(),
        tf.keras.layers.Dense(32, activation="relu"),
        tf.keras.layers.Dense(1),
    ])
    model.compile(optimizer="adam", loss="mse", metrics=["mae"])
    return model


# save test photos
def save_test_photos():
    os.makedirs(TEST_FOLDER, exist_ok=True)
    for om in [1.0, 2.0, 3.0, 4.5, 6.5]:
        img = make_fake_photo(om, 256)
        name = "soil_om_" + str(om) + ".png"
        Image.fromarray((img * 255).astype(np.uint8)).save(os.path.join(TEST_FOLDER, name))


if os.path.exists(os.path.join(REAL_FOLDER, "labels.csv")):
    print("Using real lab photos")
    pictures, answers = load_real_photos()
else:
    print("No real photos found, making practice photos")
    pictures, answers = make_practice_photos(8000)

model = make_model()
model.fit(pictures, answers, epochs=25, batch_size=64, validation_split=0.15, verbose=2)

# check model error
split = int(len(pictures) * 0.85)
loss, mae = model.evaluate(pictures[split:], answers[split:], verbose=0)
print("Average error: " + str(round(mae, 3)) + " percent organic matter")

# save for the phone
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
os.makedirs(os.path.dirname(MODEL_FILE), exist_ok=True)
open(MODEL_FILE, "wb").write(tflite_model)
print("Saved " + MODEL_FILE)

# try the phone model
save_test_photos()
interpreter = tf.lite.Interpreter(model_path=MODEL_FILE)
interpreter.allocate_tensors()
input_info = interpreter.get_input_details()[0]
output_info = interpreter.get_output_details()[0]
for name in sorted(os.listdir(TEST_FOLDER)):
    photo = Image.open(os.path.join(TEST_FOLDER, name)).convert("RGB").resize((SIZE, SIZE), Image.BILINEAR)
    data = np.array(photo, dtype=np.float32)[None, :, :, :] / 255.0
    interpreter.set_tensor(input_info["index"], data)
    interpreter.invoke()
    guess = interpreter.get_tensor(output_info["index"])[0][0]
    print(name + " -> guess " + str(round(float(guess), 2)) + "%")
