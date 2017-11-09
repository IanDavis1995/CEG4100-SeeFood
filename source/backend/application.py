import os
import numpy as np
import tensorflow as tf
import pymysql
import logging
import base64

from flask import Flask, request, json
from PIL import Image

APP_HOME = os.path.join("/var", "www", "seefood")

logging.basicConfig(filename=os.path.join(APP_HOME, 'application.log'), level=logging.DEBUG)

app = Flask(__name__)
sess = tf.Session()
saver = tf.train.import_meta_graph(os.path.join(APP_HOME, 'seefood-core-ai/saved_model/model_epoch5.ckpt.meta'))
saver.restore(sess, tf.train.latest_checkpoint(os.path.join(APP_HOME, 'seefood-core-ai/saved_model')))
graph = tf.get_default_graph()
x_input = graph.get_tensor_by_name('Input_xn/Placeholder:0')
keep_prob = graph.get_tensor_by_name('Placeholder:0')
class_scores = graph.get_tensor_by_name("fc8/fc8:0")


@app.route("/gallery")
def get_gallery():
    return "Hello World!"


@app.route("/searchCriteria", methods=['POST'])
def parseJSON():
    # connect to DB
    myConnection = pymysql.connect(user='root', host='localhost', db='seefood')
    cur = myConnection.cursor()
    
    # extract from JSON body
    data = request.data
    dataDict = json.loads(data)
    name = dataDict['name']
    classType = dataDict['class']

    # format and execute query
    query = "SELECT name FROM imageuploads WHERE name='%s' AND class='%s'" %(name, classType)
    cur.execute(query)

    # return image
    results = []

    for name in cur.fetchall():
        results.append(name)

    image_path = "/var/www/seefood/uploaded_images/%s.jpg" %(results[0])
    uploaded_file = Image.open(image_path)
    return uploaded_file.tobytes()


@app.route("/analyze_json", methods=["POST"])
def analyze_json():
    json_request = json.loads(request.data)

    filedata = base64.b64decode(json_request["data"])
    filename = json_request["name"]
    filetype = json_request["type"]
    filepath = os.path.join(APP_HOME, "uploaded_images/", filename)

    logging.debug("Filedata (shrunk): " + filedata[:20])
    logging.debug("Filename: {0}".format(filename))
    logging.debug("Filetype: {0}".format(filetype))

    with open(filepath, "wb") as image_file:
        image_file.write(filedata)

    resized_filepath = os.path.join(APP_HOME, "resized_images", filename)
    image = Image.open(filepath).convert("RGB")
    image = image.resize((227, 227), Image.BILINEAR)
    image.save(resized_filepath)

    img_tensor = [np.asarray(image, dtype=np.float32)]
    scores = sess.run(class_scores, {x_input: img_tensor, keep_prob: 1.})

    return json.dumps({
        "name": filename,
        "type": filetype,
        "contains_food": not np.argmax(scores) == 1,
        "certainty": "95",
    })


@app.route("/analyze", methods=['POST'])
def analyze():
    if 'file' not in request.files:
        return "No File Found!"

    uploaded_file = request.files['file']
    path_name = os.path.join(APP_HOME, 'uploaded_images/', str(uploaded_file.filename))
    uploaded_file.save(path_name)
    os.chmod(path_name, 0777)

    image_path = path_name
    image = Image.open(image_path).convert('RGB')
    image = image.resize((227, 227), Image.BILINEAR)
    resized_path = os.path.join(APP_HOME, 'resized_images/', path_name.split('/')[-1])
    image.save(resized_path, "PNG")

    # Work in RGBA space (A=alpha) since png's come in as RGBA, jpeg come in as RGB
    # so convert everything to RGBA and then to RGB.
    image_path = path_name
    image = Image.open(image_path).convert('RGB')
    image = image.resize((227, 227), Image.BILINEAR)
    img_tensor = [np.asarray(image, dtype=np.float32)]

    # Run the image in the model.
    scores = sess.run(class_scores, {x_input: img_tensor, keep_prob: 1.})

    return json.dumps({
        "name": uploaded_file.filename,
        "type": "jpg",
        "contains_food": not np.argmax(scores) == 1,
        "certainty": "95"
    })


if __name__ == "__main__":
    print "running app"
    app.run("0.0.0.0", 80, debug=True)
