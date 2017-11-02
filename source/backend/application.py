import os
from flask import Flask, request
#from werkzeug.utils import secure_filename
import argparse
import numpy as np
import tensorflow as tf
from PIL import Image
import json
import pymysql


app = Flask(__name__)


@app.route("/gallery")
def get_gallery():
    return "Hello World!"

@app.route("/searchCriteria", methods=['POST'])
def parseJSON():
    #connect to DB
    myConnection = pymysql.connect(user='root', host='localhost', db='seefood')
    cur = myConnection.cursor()
    
    #extract from JSON body
    data = request.data
    dataDict = json.loads(data)
    name = dataDict['name']
    classType = dataDict['class']

    #format and execute query
    query = "SELECT name FROM imageuploads WHERE name='%s' AND class='%s'" %(name, classType)
    cur.execute(query)

    #return image
    results = []
    for name in cur.fetchall():
      results.append(name)
    image_path = "/var/www/seefood/uploaded_images/%s.jpg" %(results[0])
    file = Image.open(image_path)
    return file.tobytes()

@app.route("/analyze", methods=['POST'])
def analyze():
    if 'file' in request.files:
      file = request.files['file']
      path_name = os.path.join('/var/www/seefood/uploaded_images/', str(file.filename))
      file.save(path_name)
      os.chmod(path_name, 0777)

      image_path = path_name
      image = Image.open(image_path).convert('RGB')
      image = image.resize((227, 227), Image.BILINEAR)
      img_tensor = [np.asarray(image, dtype=np.float32)]
      resized_path = os.path.join('/var/www/seefood/resized_images/', path_name.split('/')[-1])
      image.save(resized_path, "PNG")
      
      sess = tf.Session()
      saver = tf.train.import_meta_graph('/var/www/seefood/seefood-core-ai/saved_model/model_epoch5.ckpt.meta')
      saver.restore(sess, tf.train.latest_checkpoint('/var/www/seefood/seefood-core-ai/saved_model'))
      graph = tf.get_default_graph()
      x_input = graph.get_tensor_by_name('Input_xn/Placeholder:0')
      keep_prob = graph.get_tensor_by_name('Placeholder:0')
      class_scores = graph.get_tensor_by_name("fc8/fc8:0")
      ######

      # Work in RGBA space (A=alpha) since png's come in as RGBA, jpeg come in as RGB
      # so convert everything to RGBA and then to RGB.
      image_path = path_name
      image = Image.open(image_path).convert('RGB')
      image = image.resize((227, 227), Image.BILINEAR)
      img_tensor = [np.asarray(image, dtype=np.float32)]
      #print 'looking for food in '+image_path

      #Run the image in the model.
      scores = sess.run(class_scores, {x_input: img_tensor, keep_prob: 1.})
      if np.argmax(scores) == 1:
        return "No Food Here Bitch"
      else:
        return "Oh yes... I see food! :D"


#      return "File Saved Locally!"
    else:
      return "No File Found"


if __name__ == "__main__":
    app.run()
