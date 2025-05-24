from flask import Flask
from tables import db, User, UserVariables, Meal, Ingredient, MealIngredient, Vital, InsulinInjected, Prediction
from flask import jsonify
from flask import request
from passlib.context import CryptContext
import openai
import traceback

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
        print("📩 JSON recibido:", request.json)

        # ➡️ Elimina o corrige el ping
        # db.session.execute(text("SELECT 1"))

        user = db.session.get(User, request.json["user_email"])
        if user is None:
            return "User not found", 404

        vital = Vital(
            user_email      = user.email,
            vital_time      = request.json.get("vital_time"),
            glucose_value   = request.json.get("glucose_value"),
            heart_rate      = request.json.get("heart_rate"),
            temperature     = request.json.get("temperature"),
            calories        = request.json.get("calories"),
            sleep_duration  = request.json.get("sleep_duration"),
            oxygen_saturation = request.json.get("oxygen_saturation"),
        )

        db.session.add(vital)
        db.session.commit()
        return "ok", 201

    except Exception as e:
        db.session.rollback()
        print("❌ ERROR en /vital:", str(e))
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

#Get all vitals for a user
@api.route("/vital/all/<string:user_email>", methods=["GET"])
def get_all_vitals(user_email):
    try:
        vitals = (
            Vital.query
            .filter_by(user_email=user_email)
            .order_by(Vital.vital_time.asc())
            .all()
        )

        if not vitals:
            return jsonify([]), 200

        return jsonify([v.serialize() for v in vitals]), 200

    except Exception as e:
        print("❌ Error in get_all_vitals:", str(e))
        return jsonify({"error": "Internal Server Error"}), 500


#---------------------MEAL----------------------

#-------------------INSULIN---------------------
@api.route("/insulin", methods=["POST"])
def post_insulin():
    user = db.session.get(User, request.json["user_email"])
    if user is None:
        return "User not found", 404

    insulin = InsulinInjected(
        user_email = user.email,
        in_time    = request.json["insulin_time"],
        in_units   = request.json["insulin_value"],
        insulin_type = request.json["insulin_type"]
    )
    db.session.add(insulin)
    db.session.commit()
    return "ok", 201


#---------------------CHATGPT----------------------
client = openai.OpenAI(api_key = "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")  # tu token aquí, SOLO en backend

@api.route("/generate_summary", methods=["POST"])
def generate_summary():
    data = request.get_json()
    user_email = data.get("user_email")
    values = data.get("values")

    if not user_email or not values:
        return {"error": "Missing data"}, 400

    try:
        prompt = f"""
        Analyze the following glucose data from the last 30 days:

        {values}

        Provide a medical-style summary of the patient's glucose progression.
        Mention stability, fluctuations, possible causes, and advice.
        """

        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "user", "content": prompt}
            ]
        )

        result = response.choices[0].message.content
        return {"summary": result.strip()}, 200

    except Exception as e:
        print("❌ ChatGPT error:", e)
        traceback.print_exc()
        return {"error": "GPT request failed"}, 500

#---------------------MAIN----------------------
if __name__ == "__main__":
    api.run(host="0.0.0.0", port=5050)