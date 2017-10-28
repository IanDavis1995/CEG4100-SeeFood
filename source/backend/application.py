from flask import Flask


app = Flask(__name__)


@app.route("/gallery")
def get_gallery():
    return "Hello World!"


@app.route("/analyze")
def analyze():
    return "Hello World!"


if __name__ == "__main__":
    app.run()
