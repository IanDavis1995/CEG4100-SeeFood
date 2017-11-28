import io
import os
import numpy as np
import tensorflow as tf
import pymysql
import logging
import base64
import datetime

from flask import Flask, request, json
from PIL import Image

APP_HOME = os.path.join("/var", "www", "seefood")

# logging.basicConfig(filename=os.path.join(APP_HOME, 'application.log'), level=logging.DEBUG)

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


@app.route("/search", methods=['POST'])
def parseJSON():
    # connect to DB
    myConnection = pymysql.connect(user='root', host='localhost', db='seefood')
    cur = myConnection.cursor()

    # extract from JSON body
    data = request.data
    dataDict = json.loads(data)
    name = dataDict['name']
    classType = dataDict['food']
    timePeriod = dataDict['time']

    # format and execute query
    query = queryGenerator(name, classType, timePeriod)
    # return query
    cur.execute(query)

    # return images and all relevant metadata
    jsonObj = {}
    i = 0

    for result in cur.fetchall():
        jsonObj[str('file%s' % i)] = {}
        jsonObj[str('file%s' % i)]['name'] = result[0]
        jsonObj[str('file%s' % i)]['contains_food'] = result[1] == "\x01"
        jsonObj[str('file%s' % i)]['certainty'] = result[4]
        jsonObj[str('file%s' % i)]['type'] = 'jpg'
        jsonObj[str('file%s' % i)]['uploadDate'] = result[2]
        imgByteArray = io.BytesIO()
        img = Image.open(result[3])
        img.save(imgByteArray, format="JPEG")
        imgByteArray = imgByteArray.getvalue()
        jsonObj[str('file%s' % i)]['data'] = base64.b64encode(imgByteArray)
        i += 1

    return json.dumps(jsonObj)


def queryGenerator(name, classType, timePeriod):
    # in progress
    today = datetime.datetime.now().date()
    query = "SELECT name, class, upload_time, img_pth, confidence FROM imageuploads WHERE "

    if name:
        query += "name='%s' " % name

        if classType or timePeriod:
            query += "AND "

    if classType:
        if classType == "T":
            query += "class='1' "
        else:
            query += "class='0'"
        if timePeriod:
            query += "AND "

    if timePeriod:
        if timePeriod == "week":
            searchDate = today - datetime.timedelta(days=7)
        elif timePeriod == "month":
            searchDate = today - datetime.timedelta(days=30)  # for now
        else:
            searchDate = today - datetime.timedelta(days=7)

        query += "upload_time>'%s'" % searchDate

    if name == '' and timePeriod == '' and classType == '':
        query = "SELECT name, class, upload_time, img_pth, confidence FROM imageuploads"

    return query


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
    confidence = abs(scores[0][0] - scores[0][1]) / 5 * 100

    # connect to DB
    myConnection = pymysql.connect(user='root', host='localhost', db='seefood')
    cur = myConnection.cursor()

    user = "user"
    date = datetime.datetime.now()
    classType = int(not np.argmax(scores) == 1)
    name = filename.split(".")[-2]

    query = "INSERT INTO imageuploads (name, class, upload_time, user, confidence, thumbnail_pth, img_pth) VALUES ('%s', %s, '%s', '%s', %f, '%s', '%s')" % (name, classType, date, user, confidence, resized_filepath, resized_filepath)
    cur.execute(query)
    myConnection.commit()
    return json.dumps({
        "name": filename,
        "type": filetype,
        "contains_food": not np.argmax(scores) == 1,
        "certainty": confidence
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
    image = image.resize((227, 227), Image.ANTIALIAS)
    resized_path = os.path.join(APP_HOME, 'resized_images/', path_name.split('/')[-1])
    image.save(resized_path, "JPEG")

    # Work in RGBA space (A=alpha) since png's come in as RGBA, jpeg come in as RGB
    # so convert everything to RGBA and then to RGB.
    image_path = path_name
    image = Image.open(image_path).convert('RGB')
    image = image.resize((227, 227), Image.BILINEAR)
    img_tensor = [np.asarray(image, dtype=np.float32)]

    # Run the image in the model.
    scores = sess.run(class_scores, {x_input: img_tensor, keep_prob: 1.})
    confidence = abs(scores[0][0] - scores[0][1]) / 5 * 100

    # connect to DB
    myConnection = pymysql.connect(user='root', host='localhost', db='seefood')
    cur = myConnection.cursor()

    user = "user"
    date = datetime.datetime.now()
    classType = int(not np.argmax(scores) == 1)
    name = uploaded_file.filename.split(".")[-2]

    query = "INSERT INTO imageuploads (name, class, upload_time, user, confidence, thumbnail_pth, img_pth) VALUES ('%s', %s, '%s', '%s', %f, '%s', '%s')" % (name, classType, date, user, confidence, image_path, image_path)
    cur.execute(query)
    myConnection.commit()

    return json.dumps({
        "name": uploaded_file.filename,
        "type": "jpg",
        "contains_food": not np.argmax(scores) == 1,
        "certainty": confidence
    })


if __name__ == "__main__":
    print "running app"
    app.run("0.0.0.0", 80, debug=True)
