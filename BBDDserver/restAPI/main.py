from flask import Flask

api: Flask=Flask(__name__)

@api.route("/api")
def get_home():
    return "Running serVidor"

if __name__ == "__main__":
    api.run()