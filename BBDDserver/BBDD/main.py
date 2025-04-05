from flask import Flask
from tables import db, User, Meal, Ingredient, Vital
from flask import request

api: Flask=Flask(__name__)
api.config["SQLALCHEMY_DATABASE_URI"] = "mysql+pymysql://thomas:root@localhost:3306/mydb"
db.app=api
db.init_app(api)

@api.route("/api")
def get_home():
    return "Running serVidor"



#---------------------USER-----------------------

@api.route("/user", methods=["POST"])
def post_user():
    user = User(
        email=request.json["email"],
        password=request.json["password"],
        birth_date=request.json["birth_date"],
        sex=request.json["sex"],
        insulin_type=request.json["insulin_type"]
    )
    db.session.add(user)
    db.session.commit()
    return "ok", 200

#---------------------VITAL----------------------

@api.route("/vital", methods=["POST"])
def post_vital():
    try:
        user = User.query.get(request.json["user_email"])
        if user is None:
            return "User not found", 404
        vital = Vital(
            user_email=user.email,
            vital_time=request.json["vital_time"],
            glucose_value=request.json["glucose_value"],
            heart_rate=request.json["heart_rate"],
            temperature=request.json["temperature"],
            calories=request.json["calories"],
            diastolic=request.json["diastolic"],
            systolic=request.json["systolic"],
            is_sleeping=request.json["is_sleeping"]
        )
        db.session.add(vital)
        db.session.commit()
        return "ok", 201
    except Exception as e:
        db.session.rollback()
        print("ERROR in /vital:", str(e))
        return "Internal Server Error", 500

@api.route("/vital/<int:vital_id>", methods=["GET"])
def get_vital_by_id(vital_id:int):
    vital = Vital.query.get(vital_id)
    if vital is None:
        return "Vital not found", 404
    return vital.serialize(), 200

#---------------------MAIN----------------------
if __name__ == "__main__":
    api.run(host="0.0.0.0", port=5050)