from flask import Flask
from tables import db, User, UserVariables, Meal, Ingredient, MealIngredient, Vital, InsulinInjected, Prediction
from flask import jsonify
from flask import request
from passlib.context import CryptContext

api: Flask=Flask(__name__)
api.config["SQLALCHEMY_DATABASE_URI"] = "mysql+pymysql://blestod:spyro123@blestod.mysql.eu.pythonanywhere-services.com/blestod$beewell"
db.app=api
db.init_app(api)

@api.route("/api")
def get_home():
    return "Running serVidor"

# Setup para bcrypt
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
# Login
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
        sex=request.json.get("sex")
    )
    db.session.add(user)
    db.session.commit()
    return jsonify({"success": True, "message": "Usuario creado"}), 200

# Get user by email
@api.route("/user/<string:user_email>", methods=["GET"])
def get_user(user_email):
    user = db.session.get(User, user_email)
    if user is None:
        return "User not found", 404
    return user.serialize(), 200

# Update userinfo
@api.route("/user/<string:user_email>", methods=["PUT"])
def update_userinfo(user_email):
    user = db.session.get(User, user_email)
    if user is None:
        return jsonify({"error": "User not found"}), 404

    data = request.get_json()

    if "sex" in data:
        user.sex = data["sex"]
    if "birth_date" in data:
        user.birth_date = data["birth_date"]

    db.session.commit()

    return user.serialize(), 200

#-----------------USER_VARIABLES-----------------
@api.route("/user_variables", methods=["POST"])
def post_user_variables():
    user = db.session.get(User, request.json["user_email"])
    if user is None:
        return "User not found", 404

    user_variable = UserVariables(
        user_email=user.email,
        change_date_time=request.json["change_date_time"],
        height=request.json["height"],
        weight=request.json["weight"]
    )
    db.session.add(user_variable)
    db.session.commit()
    return jsonify({
        "saved": {
            "height": request.json.get("height"),
            "weight": request.json.get("weight")
        }
    }), 201



@api.route("/user_variables/last/<string:user_email>", methods=["GET"])
def get_last_user_variable(user_email):
    user_variable = (
        UserVariables.query
        .filter_by(user_email=user_email)
        .order_by(UserVariables.user_var.desc())
        .first()
    )
    if user_variable is None:
        return jsonify({
            "height": None,
            "weight": None
        }), 200

    return user_variable.serialize(), 200

#---------------------VITAL----------------------

@api.route("/vital", methods=["POST"])
def post_vital():
    try:
        user = db.session.get(User, request.json["user_email"])
        if user is None:
            return "User not found", 404

        vital = Vital(
            user_email=user.email,
            vital_time=request.json.get("vital_time"),
            glucose_value=request.json.get("glucose_value"),
            heart_rate=request.json.get("heart_rate"),
            temperature=request.json.get("temperature"),
            calories=request.json.get("calories"),
            sleep_duration=request.json.get("sleep_duration"),
            oxygen_saturation=request.json.get("oxygen_saturation")
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

@api.route("/vital/latest/<string:user_email>", methods=["GET"])
def get_latest_vital(user_email):
    vital = (
        Vital.query
        .filter_by(user_email=user_email)
        .order_by(Vital.vital_id.desc())
        .first()
    )
    if vital is None:
        return "No vitals found for this user", 404
    return vital.serialize(), 200

#---------------------MEAL----------------------

#-------------------INSULIN---------------------
@api.route("/insulin", methods=["POST"])
def post_insulin():
    user = db.session.get(User, request.json["user_email"])
    if user is None:
        return "User not found", 404
    insulin = InsulinInjected(
        user_email=user.email,
        insulin_time=request.json["insulin_time"],
        insulin_value=request.json["insulin_value"],
        insulin_type=request.json["insulin_type"]
    )
    db.session.add(insulin)
    db.session.commit()
    return "ok", 201

#---------------------MAIN----------------------
if __name__ == "__main__":
    api.run(host="0.0.0.0", port=5050)