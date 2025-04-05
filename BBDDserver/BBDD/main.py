from flask import Flask
from tables import db, User, Meal, Ingredient, Vital
from flask import jsonify
from flask import request
from passlib.context import CryptContext

api: Flask=Flask(__name__)
api.config["SQLALCHEMY_DATABASE_URI"] = "mysql+pymysql://thomas:root@localhost:3306/mydb"
db.app=api
db.init_app(api)

@api.route("/api")
def get_home():
    return "Running serVidor"

# Setup para bcrypt
# Login
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def hash_password(password):
    return pwd_context.hash(password)

def login(email, password):
    user = User.query.filter_by(email=email).first()
    if user and verify_password(password, user.password):
        return user
    return None

@api.route("/login", methods=["POST"])
def login_route():
    data = request.json
    email = data.get("email")
    password = data.get("password")

    user = User.query.filter_by(email=email).first()
    if user and verify_password(password, user.password):
        return {"login": True, "user_email": user.email}, 200
    else:
        return {"login": False}, 401

#---------------------USER-----------------------

# Register
@api.route("/user", methods=["POST"])
def post_user():
    user = User(
        email=request.json.get("email"),
        password = hash_password(request.json.get("password")),
        birth_date=request.json.get("birth_date"),
        sex=request.json.get("sex"),
        insulin_type=request.json.get("insulin_type")
    )
    db.session.add(user)
    db.session.commit()
    return jsonify({"success": True, "message": "Usuario creado"}), 200

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