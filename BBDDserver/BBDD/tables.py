from flask_sqlalchemy import SQLAlchemy

db: SQLAlchemy = SQLAlchemy()

class User(db.Model):
    __tablename__="user"

    email = db.Column(db.String(45), primary_key = True)
    password = db.Column(db.String(60))
    birth_date = db.Column(db.Integer, nullable=True)
    sex = db.Column(db.Boolean, nullable=True)

    #db.relationship("nombre de la tabla","backref = x->crea una variable en tabla hijo que se llame x y que referencie al padre para meal.user o user.meal","al eliminar usuario se borran hijos")
    meals = db.relationship("Meal",backref="user",cascade="all,delete")
    vitals = db.relationship("Vital",backref="user",cascade="all,delete")
    predictions = db.relationship("Prediction",backref="user",cascade="all,delete")
    user_variables = db.relationship("UserVariables",backref="user",cascade="all,delete")
    insulin_injections = db.relationship("InsulinInjected", backref="user", cascade="all, delete")
    activities = db.relationship("Activity", backref="user", cascade="all,delete")

    def __init__(self, email, password, birth_date, sex):
        self.email = email
        self.password = password
        self.birth_date = birth_date
        self.sex = sex

    def serialize(self):
        return {
            "email": self.email,
            "password": self.password,
            "birth_date": self.birth_date,
            "sex": self.sex,
            }
    
class UserVariables(db.Model):
        __tablename__="user_variables"

        user_var = db.Column(db.Integer,primary_key = True, autoincrement = True)
        change_date_time = db.Column(db.Integer)
        height = db.Column(db.Float, nullable=True)
        weight = db.Column(db.Float, nullable=True)
        insulin_sensitivity = db.Column(db.Float, nullable=True)
        carb_ratio = db.Column(db.Float, nullable=True)
        carb_absorption_rate = db.Column(db.Float, nullable=True)
        diabetes_type = db.Column(db.Integer, nullable=True)

        user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

        def __init__(self, user_email, change_date_time, height, weight, insulin_sensitivity, carb_ratio, carb_absorption_rate, diabetes_type=None):
            self.user_email = user_email
            self.change_date_time = change_date_time
            self.height = height
            self.weight = weight
            self.insulin_sensitivity = insulin_sensitivity
            self.carb_ratio = carb_ratio
            self.carb_absorption_rate = carb_absorption_rate
            self.diabetes_type = diabetes_type

        def serialize(self):
            return {
                "user_var": self.user_var,
                "change_date_time": self.change_date_time,
                "height": self.height,
                "weight": self.weight,
                "insulin_sensitivity": self.insulin_sensitivity,
                "carb_ratio": self.carb_ratio,
                "carb_absorption_rate": self.carb_absorption_rate,
                "diabetes_type": self.diabetes_type,
            }

class InsulinInjected(db.Model):
    __tablename__ = "insulin_injected"

    injected_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    in_time     = db.Column(db.Integer, nullable=False)
    in_units    = db.Column(db.Float,   nullable=False)
    insulin_type = db.Column(db.String(45), nullable=False)
    in_spot = db.Column(db.String(45), nullable=False)

    user_email = db.Column(db.String(45), db.ForeignKey("user.email"), nullable=False)


    def __init__(self, user_email, in_time, in_units, insulin_type, in_spot):
        self.user_email = user_email
        self.in_time = in_time
        self.in_units = in_units
        self.insulin_type = insulin_type
        self.in_spot = in_spot

    def serialize(self):
        return {
            "injected_id": self.injected_id,
            "in_time": self.in_time,
            "in_units": self.in_units,
            "insulin_type": self.insulin_type,
            "user_email": self.user_email,
            "in_spot": self.in_spot
        }

#---------------------MEAL----------------------
class Meal(db.Model):
    __tablename__ = "meal"

    meal_id   = db.Column(db.Integer, primary_key=True, autoincrement=True)
    meal_time = db.Column(db.Integer,  nullable=False)      # consider DateTime in the future
    grams     = db.Column(db.Float,    nullable=False)

    user_email = db.Column(db.String(45),
                           db.ForeignKey("user.email"),
                           nullable=False)

    food_id = db.Column(
        'food_food_id',
        db.Integer,
        db.ForeignKey("food.food_id"),
        nullable=False
    )

    food = db.relationship("Food", back_populates="meals")

    def __init__(self, user_email, meal_time, grams, food_id):
        self.user_email = user_email
        self.meal_time  = meal_time
        self.grams      = grams
        self.food_id    = food_id

    def serialize(self):
        return {
            "meal_id":   self.meal_id,
            "meal_time": self.meal_time,
            "grams":     self.grams,
            "user_email": self.user_email,
            "food_id":   self.food_id
        }

#---------------------FOOD----------------------
class Food(db.Model):
    __tablename__ = "food"

    food_id   = db.Column(db.Integer, primary_key=True, autoincrement=True)
    food_name      = db.Column(db.String(255), unique=True, nullable=False)

    estim_carbs     = db.Column(db.Float)
    estim_protein   = db.Column(db.Float)
    estim_fats      = db.Column(db.Float)
    i_g        = db.Column(db.Float)

    def __init__(self, food_name, estim_carbs=None, estim_protein=None, estim_fats=None, i_g=None):
        self.food_name = food_name
        self.estim_carbs = estim_carbs
        self.estim_protein = estim_protein
        self.estim_fats = estim_fats
        self.i_g = i_g

    def serialize(self):
        return {
            "food_id": self.food_id,
            "food_name": self.food_name,
            "estim_carbs": self.estim_carbs,
            "estim_protein": self.estim_protein,
            "estim_fats": self.estim_fats,
            "i_g": self.i_g
        }
    
    meals = db.relationship("Meal", back_populates="food")  

#---------------------VITAL----------------------
class Vital(db.Model):
    __tablename__ = "vital"

    vital_id   = db.Column(db.Integer, primary_key=True, autoincrement=True)
    vital_time = db.Column(db.Integer,  nullable=False)

    glucose_value     = db.Column(db.Float,  nullable=True)
    heart_rate        = db.Column(db.Float,  nullable=True)
    temperature       = db.Column(db.Float,  nullable=True)
    calories          = db.Column(db.Float,  nullable=True)
    sleep_duration    = db.Column(db.Float,  nullable=True)
    oxygen_saturation = db.Column(db.Float,  nullable=True)

    user_email = db.Column(db.String(45), db.ForeignKey("user.email"))

    def __init__(
        self, user_email, vital_time,
        glucose_value=None, heart_rate=None, temperature=None,
        calories=None, sleep_duration=None, oxygen_saturation=None
    ):
        self.user_email        = user_email
        self.vital_time        = vital_time
        self.glucose_value     = glucose_value
        self.heart_rate        = heart_rate
        self.temperature       = temperature
        self.calories          = calories
        self.sleep_duration    = sleep_duration
        self.oxygen_saturation = oxygen_saturation

    def serialize(self):
        return {
            "vital_id":          self.vital_id,
            "vital_time":        self.vital_time,
            "glucose_value":     self.glucose_value,
            "heart_rate":        self.heart_rate,
            "temperature":       self.temperature,
            "calories":          self.calories,
            "sleep_duration":    self.sleep_duration,
            "oxygen_saturation": self.oxygen_saturation,
        }
    
#---------------------ACTIVITY----------------------

class Activity(db.Model):
    __tablename__ = "activity"

    activity_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    act_name        = db.Column(db.String(100), nullable=True)
    duration_min = db.Column(db.Integer, nullable=False)
    intensity   = db.Column(db.String(20), nullable=False)  # "light", "moderate", "vigorous"
    act_time   = db.Column(db.Integer, nullable=False)
    activity_type = db.Column(db.String(50), nullable=True)  # "aerobic, anaerobic, flexibility, balance"

    user_email  = db.Column(db.String(45), db.ForeignKey("user.email"), nullable=False)

    def __init__(self, user_email, duration_min, intensity, act_time, activity_type=None, act_name= None):
        self.user_email = user_email
        self.act_name = act_name
        self.duration_min = duration_min
        self.intensity = intensity
        self.act_time = act_time
        self.activity_type = activity_type

    def serialize(self):
        return {
            "activity_id": self.activity_id,
            "user_email": self.user_email,
            "act_name": self.act_name,
            "duration_min": self.duration_min,
            "intensity": self.intensity,
            "act_time": self.act_time,
            "activity_type": self.activity_type
        }



#---------------------PREDICTION----------------------

class Prediction(db.Model):
    __tablename__="prediction"

    prediction_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    predict_time = db.Column(db.Integer)
    confidence_lvl = db.Column(db.Integer, nullable=True)
    forecast_time = db.Column(db.Integer,  nullable=True)
    forecast_desc = db.Column(db.String(300))
    predict_type = db.Column(db.Integer)


    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

    def __init__(self, user_email, predict_time, forecast_desc, predict_type, confidence_lvl=None, forecast_time=None):
        self.user_email = user_email
        self.predict_time = predict_time
        self.confidence_lvl = confidence_lvl
        self.forecast_time = forecast_time
        self.forecast_desc = forecast_desc
        self.predict_type = predict_type

    def serialize(self):
        return {
            "prediction_id": self.prediction_id,
            "predict_time": self.predict_time,
            "confidence_lvl": self.confidence_lvl,
            "forecast_time": self.forecast_time,
            "forecast_desc": self.forecast_desc,
            "predict_type": self.predict_type,
            "user_email": self.user_email
        }
