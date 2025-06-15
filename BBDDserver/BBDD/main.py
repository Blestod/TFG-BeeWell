from flask import Flask
from tables import Activity, db, User, UserVariables, Meal, Food, Vital, InsulinInjected, Prediction
from flask import jsonify
from flask import request
from passlib.context import CryptContext
import openai
import traceback
from datetime import datetime, timezone, timedelta
import numpy as np
from flask import abort
import json


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
        carb_absorption_rate=request.json.get("carb_absorption_rate"),
        diabetes_type=request.json.get("diabetes_type"),
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
            "carb_absorption_rate": None,
            "diabetes_type": None
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

#-----------rec
@api.route("/meal_recommendation_data/<string:user_email>", methods=["GET"])
def get_meal_recommendation_data(user_email):
    try:
        now = datetime.now(timezone.utc)
        now_epoch = int(now.timestamp())
        current_hour = now.hour

        # Definir franjas horarias
        if 6 <= current_hour < 12:
            meal_name = "breakfast"
            start_time = now.replace(hour=6, minute=0, second=0)
        elif 12 <= current_hour < 17:
            meal_name = "lunch"
            start_time = now.replace(hour=12, minute=0, second=0)
        else:
            meal_name = "dinner"
            if current_hour < 6:
                start_time = now.replace(hour=17, minute=0, second=0) - timedelta(days=1)
            else:
                start_time = now.replace(hour=17, minute=0, second=0)

        start_epoch = int(start_time.timestamp())

        # Obtener todas las comidas registradas en la franja
        meals = (Meal.query
                 .filter_by(user_email=user_email)
                 .filter(Meal.meal_time >= start_epoch)
                 .filter(Meal.meal_time <= now_epoch)
                 .all())

        events = []

        for meal in meals:
            food = Food.query.get(meal.food_id)
            if not food:
                continue

            grams = meal.grams

            composition = {
                "carbs": round((food.estim_carbs or 0.0) * grams / 100, 1),
                "protein": round((food.estim_protein or 0.0) * grams / 100, 1),
                "fat": round((food.estim_fats or 0.0) * grams / 100, 1),
                "ingredients": [
                    {
                        "item": food.food_name,
                        "quantity": f"{int(grams)} grams"
                    }
                ]
            }

            meal_time = meal.meal_time

            # Glucosa ±3h respecto a esta comida
            pre_glucose = []
            post_glucose = []

            vitals = (Vital.query
                      .filter_by(user_email=user_email)
                      .filter(Vital.glucose_value != None)
                      .order_by(Vital.vital_time.asc())
                      .all())

            for v in vitals:
                delta = v.vital_time - meal_time
                if -3 * 3600 <= delta < 0:
                    pre_glucose.append(v.glucose_value)
                elif 0 <= delta <= 3 * 3600:
                    post_glucose.append(v.glucose_value)

            event = {
                "type": "meal",
                "name": meal_name,
                "time": datetime.utcfromtimestamp(meal_time).isoformat(),
                "duration": "20min",
                "composition": composition,
                "glucose_levels": {
                    "pre_3h": pre_glucose,
                    "post_3h": post_glucose
                }
            }

            events.append(event)

        return jsonify(events), 200

    except Exception as e:
        print("❌ Error en /meal_recommendation_data:", e)
        traceback.print_exc()
        return jsonify({"error": "internal server error"}), 500


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
        insulin_type=request.json["insulin_type"],
        in_spot=request.json["in_spot"]
    )
    db.session.add(insulin)
    db.session.commit()

    return jsonify({"status": "ok"}), 201



#-----------rec

@api.route("/insulin_recommendation_data/<string:user_email>", methods=["GET"])
def get_last_insulin_injection(user_email):
    try:
        now_epoch = int(datetime.now(timezone.utc).timestamp())

        injection = (InsulinInjected.query
                     .filter_by(user_email=user_email)
                     .order_by(InsulinInjected.in_time.desc())
                     .first())

        if not injection:
            return jsonify({}), 200

        insulin_type = injection.insulin_type
        in_time = injection.in_time
        delta = now_epoch - in_time

        # Filtro por tipo y ventana de tiempo
        if insulin_type == "rapid-acting" and delta > 4 * 3600:
            return jsonify({}), 200
        if insulin_type == "slow-acting" and delta > 24 * 3600:
            return jsonify({}), 200

        # Convertimos epoch a hora UTC
        injection_dt = datetime.utcfromtimestamp(in_time)
        hour = injection_dt.hour

        if 6 <= hour < 12:
            name = "morning_dose"
        elif 12 <= hour < 17:
            name = "afternoon_dose"
        elif 17 <= hour < 22:
            name = "evening_dose"
        else:
            name = "night_dose"

        # Valor de glucosa más cercano
        glucose_value = None
        closest_vital = (Vital.query
                         .filter_by(user_email=user_email)
                         .filter(Vital.glucose_value != None)
                         .order_by(db.func.abs(Vital.vital_time - in_time))
                         .first())

        if closest_vital:
            glucose_value = closest_vital.glucose_value

        vitals_window_before = []
        vitals_window_after = []
        pre_glucose = []
        post_glucose = []

        vitals = (Vital.query
                  .filter_by(user_email=user_email)
                  .order_by(Vital.vital_time.asc())
                  .all())

        for v in vitals:
            delta_t = v.vital_time - in_time
            vital_data = {
                "timestamp": datetime.utcfromtimestamp(v.vital_time).isoformat(),
                "heart_rate": v.heart_rate,
                "temperature": v.temperature,
                "oxygen_saturation": v.oxygen_saturation,
                "calories": v.calories,
                "sleep_duration": v.sleep_duration
            }

            if -3 * 3600 <= delta_t < 0:
                if v.glucose_value is not None:
                    pre_glucose.append(v.glucose_value)
                vitals_window_before.append(vital_data)
            elif 0 <= delta_t <= 3 * 3600:
                if v.glucose_value is not None:
                    post_glucose.append(v.glucose_value)
                vitals_window_after.append(vital_data)

        insulin_event = {
            "type": "insulin_injection",
            "name": name,
            "time": injection_dt.isoformat(),
            "insulin_type": insulin_type,
            "insulin_dose": injection.in_units,
            "injection_site": injection.in_spot,
            "glucose_level_at_injection": glucose_value,
            "glucose_levels": {
                "pre_3h": pre_glucose,
                "post_3h": post_glucose
            },
            "vitals_window": {
                "pre_3h": vitals_window_before,
                "post_3h": vitals_window_after
            }
        }

        return jsonify(insulin_event), 200

    except Exception as e:
        print("❌ Error en /insulin_recommendation_data:", e)
        traceback.print_exc()
        return jsonify({"error": "internal server error"}), 500


#---------------------ACTIVIY----------------------
@api.route("/activity", methods=["POST"])
def post_activity():
    try:
        user = db.session.get(User, request.json["user_email"])
        if user is None:
            return jsonify({"error": "User not found"}), 404

        activity = Activity(
            user_email=user.email,
            act_name=request.json.get("act_name"),
            duration_min=request.json["duration_min"],
            intensity=request.json["intensity"],
            act_time=request.json["act_time"],
            activity_type=request.json.get("activity_type")
        )
        db.session.add(activity)
        db.session.commit()

        return jsonify({"status": "ok"}), 201

    except Exception as e:
        db.session.rollback()
        print("❌ Error saving activity:", e)
        return jsonify({"error": "internal server error"}), 500


#--------------rec
@api.route("/exercise_recommendation_data/<string:user_email>", methods=["GET"])
def get_exercise_events(user_email):
    try:
        today_6am = datetime.now(timezone.utc).replace(hour=6, minute=0, second=0, microsecond=0)
        start_epoch = int(today_6am.timestamp())

        # Últimas 24h de actividades
        activities = (
            Activity.query
            .filter_by(user_email=user_email)
            .filter(Activity.act_time >= start_epoch)
            .order_by(Activity.act_time.asc())
            .all()
        )

        # Signos vitales ordenados
        vitals = (
            Vital.query
            .filter_by(user_email=user_email)
            .filter(Vital.glucose_value != None)
            .order_by(Vital.vital_time.asc())
            .all()
        )

        def get_glucose_window(ts):
            pre, post = [], []
            for v in vitals:
                delta = v.vital_time - ts
                if -3 * 3600 <= delta < 0:
                    pre.append(v.glucose_value)
                elif 0 <= delta <= 3 * 3600:
                    post.append(v.glucose_value)
            return pre, post

        events = []

        for a in activities:
            pre_glucose, post_glucose = [], []
            vitals_window_before, vitals_window_after = [], []

            for v in vitals:
                delta = v.vital_time - a.act_time
                vital_data = {
                    "timestamp": datetime.utcfromtimestamp(v.vital_time).isoformat(),
                    "heart_rate": v.heart_rate,
                    "temperature": v.temperature,
                    "oxygen_saturation": v.oxygen_saturation,
                    "calories": v.calories,
                    "sleep_duration": v.sleep_duration
                }

                if -3 * 3600 <= delta < 0:
                    if v.glucose_value is not None:
                        pre_glucose.append(v.glucose_value)
                    vitals_window_before.append(vital_data)
                elif 0 <= delta <= 3 * 3600:
                    if v.glucose_value is not None:
                        post_glucose.append(v.glucose_value)
                    vitals_window_after.append(vital_data)

            event = {
                "type": "exercise",
                "name": a.act_name if a.act_name else (a.activity_type or "exercise"),
                "time": datetime.utcfromtimestamp(a.act_time).isoformat(),
                "duration": f"{a.duration_min}min",
                "intensity": a.intensity,
                "glucose_levels": {
                    "pre_3h": pre_glucose,
                    "post_3h": post_glucose
                },
                "vitals_window": {
                    "pre_3h": vitals_window_before,
                    "post_3h": vitals_window_after
                }
            }

            events.append(event)


        return jsonify(events), 200

    except Exception as e:
        print("❌ Error en /exercise_recommendation_data:", e)
        traceback.print_exc()
        return jsonify({"error": "internal server error"}), 500


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
    


@api.route("/generate_insights", methods=["POST"])
def generate_insights():
    data        = request.get_json()
    user_email  = data.get("user_email")
    vitals      = data.get("vitals")
    predictions = data.get("predictions")

    if not user_email or vitals is None or predictions is None:
        return jsonify(error="Missing data"), 400

    # Prepara los strings para el prompt
    lines = []
    if vitals.get("glucose") is not None:
        lines.append(f"Glucosa: {vitals['glucose']} mg/dL")
    if vitals.get("heart_rate") is not None:
        lines.append(f"Pulso: {vitals['heart_rate']} bpm")
    if vitals.get("temperature") is not None:
        lines.append(f"Temperatura: {vitals['temperature']} °C")
    if vitals.get("oxygen_saturation") is not None:
        lines.append(f"SpO₂: {vitals['oxygen_saturation']}%")

    preds_str = "\n".join(
        f"- {p['time']}: {p['value']} mg/dL"
        for p in predictions
    )

    prompt = f"""
    Eres un asistente médico. Recibes:
    1) Datos recientes del paciente:
       {'; '.join(lines)}.
    2) Predicción de glucosa para la próxima hora:
       {preds_str}

    Genera DOS respuestas en ingles:
    A) recommendation_vitals: 1–2 oraciones con un consejo inmediato.
    B) recommendation_prediction: un párrafo detallado sobre la predicción.

    Devuélveme solo este JSON:
    {{
      "recommendation_vitals": "...",
      "recommendation_prediction": "..."
    }}
    """

    try:
        resp = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[{"role":"user","content":prompt}],
            temperature=0.7,
            max_tokens=250
        )
        content = resp.choices[0].message.content.strip()
        print("✅ GPT raw content:", content)

        parsed = json.loads(content)
        return jsonify(
            recommendation_vitals     = parsed.get("recommendation_vitals", ""),
            recommendation_prediction = parsed.get("recommendation_prediction", "")
        ), 200

    except Exception as e:
        print("❌ GPT error in /generate_insights:", e)
        traceback.print_exc()
        # aseguramos JSON de error
        return jsonify(error="GPT request failed"), 500

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