from flask import Flask
from tables import db, User, UserVariables, Meal, Food, Vital, InsulinInjected, Prediction
from flask import jsonify
from flask import request
from passlib.context import CryptContext
import openai
import traceback
from datetime import datetime, timezone
import numpy as np
from flask import abort


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
        user_email=request.json["user_email"],
        change_date_time=request.json["change_date_time"],
        height=request.json.get("height"),
        weight=request.json.get("weight"),
        insulin_sensitivity=request.json.get("insulin_sensitivity"),
        carb_ratio=request.json.get("carb_ratio"),
        carb_absorption_rate=request.json.get("carb_absorption_rate")
    )
    db.session.add(user_variable)
    db.session.commit()
    return jsonify({"saved": user_variable.serialize()}), 201

@api.route("/user_variables/last/<string:user_email>", methods=["GET"])
def get_last_user_variable(user_email):
    user_variable = (UserVariables.query
                     .filter_by(user_email=user_email)
                     .order_by(UserVariables.user_var.desc())
                     .first())
    if user_variable is None:
        return jsonify({
            "height": None,
            "weight": None,
            "insulin_sensitivity": None,
            "carb_ratio": None,
            "carb_absorption_rate": None
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
@api.route("/food/search")
def search_food():
    query = request.args.get("q", "").lower().strip()
    if not query:
        return jsonify([]), 200

    # Basic fuzzy match using LIKE
    results = (Food.query
               .filter(Food.food_name.ilike(f"%{query}%"))
               .limit(10)
               .all())

    return jsonify([f.serialize() for f in results]), 200

@api.route("/meal", methods=["POST"])
def post_meal():
    try:
        data = request.get_json()
        meal = Meal(
            user_email=data["user_email"],
            meal_time=data["meal_time"],
            grams=data["grams"],
            food_id=data["food_id"]
        )
        db.session.add(meal)
        db.session.commit()
        return "Meal saved", 201
    except Exception as e:
        db.session.rollback()
        print("❌ Error saving meal:", e)
        return "Error saving meal", 500



#-------------------INSULIN---------------------
@api.route("/insulin", methods=["POST"])
def post_insulin():
    user = db.session.get(User, request.json["user_email"])
    if user is None:
        return jsonify({"error": "User not found"}), 404

    insulin = InsulinInjected(
        user_email=user.email,
        in_time=request.json["insulin_time"],
        in_units=request.json["insulin_value"],
        insulin_type=request.json["insulin_type"]
    )
    db.session.add(insulin)
    db.session.commit()

    return jsonify({"status": "ok"}), 201



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
    

#---------------------PREDICTION----------------------
# ────────────────────────────────────────────────────────────────
# 1 ·  ENDPOINT DE FEATURES ─ signos vitales agregados
#    GET /features/vitals/<user_email>?from=<epoch_ms>&to=<epoch_ms>
# ────────────────────────────────────────────────────────────────
@api.route("/features/vitals/<string:user_email>")
def features_vitals(user_email: str):
    try:
        start_ms = int(request.args.get("from", 0))
        end_ms   = int(request.args.get("to",   0))
        if end_ms == 0 or end_ms <= start_ms:
            abort(400, "'from' y 'to' deben ser epoch-ms válidos")

        WINDOW_MS = 5 * 60 * 1000          # 5 minutos
        n_bins = (end_ms - start_ms) // WINDOW_MS

        # 1️⃣  obtén todas las filas de Vital en ese intervalo
        vitals = (Vital.query
                  .filter_by(user_email=user_email)
                  .filter(Vital.vital_time
                          .between(start_ms // 1000, end_ms // 1000))
                  .all())

        # 2️⃣  prepara contenedores
        hr_buckets = [[] for _ in range(n_bins)]
        spo2_last  = [None] * n_bins
        temp_last  = [None] * n_bins

        for v in vitals:
            idx = (v.vital_time * 1000 - start_ms) // WINDOW_MS
            if 0 <= idx < n_bins:
                if v.heart_rate is not None:
                    hr_buckets[idx].append(v.heart_rate)
                if v.oxygen_saturation is not None:
                    spo2_last[idx] = v.oxygen_saturation
                if v.temperature is not None:
                    temp_last[idx] = v.temperature

        hr_mean = [float(np.mean(b)) if b else None for b in hr_buckets]
        hr_std  = [float(np.std(b))  if b else None for b in hr_buckets]

        return jsonify({
            "window_ms": WINDOW_MS,
            "from":      start_ms,
            "to":        end_ms,
            "features": {
                "hr_mean":   hr_mean,
                "hr_std":    hr_std,
                "spo2_last": spo2_last,
                "temp_last": temp_last
            }
        }), 200

    except Exception as e:
        print("❌ /features/vitals error:", e)
        abort(500, "internal-error")


# ────────────────────────────────────────────────────────────────
# 2 ·  ENDPOINT DE DATOS ESTÁTICOS DE USUARIO
#    GET /user/static/<user_email>
# ────────────────────────────────────────────────────────────────
@api.route("/user/static/<string:user_email>")
def static_user(user_email: str):
    try:
        user = User.query.get(user_email)
        if user is None:
            abort(404, "user-not-found")

        # edad en años (redondeo simple)
        age = None
        if user.birth_date:
            bd = datetime.fromtimestamp(user.birth_date, tz=timezone.utc)
            today = datetime.now(tz=timezone.utc)
            age = today.year - bd.year - ((today.month, today.day) < (bd.month, bd.day))

        last_vars = (UserVariables.query
                     .filter_by(user_email=user_email)
                     .order_by(UserVariables.user_var.desc())
                     .first())

        return jsonify({
            "age":    age,
            "sex":    user.sex,                 # True = F, False = M, null = desconocido
            "height": last_vars.height if last_vars else None,
            "weight": last_vars.weight if last_vars else None
        }), 200

    except Exception as e:
        print("❌ /user/static error:", e)
        abort(500, "internal-error")


#---------------------MAIN----------------------
if __name__ == "__main__":
    api.run(host="0.0.0.0", port=5050)