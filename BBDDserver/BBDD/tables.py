from flask_sqlalchemy import SQLAlchemy

db: SQLAlchemy = SQLAlchemy()

class User(db.Model):
    __tablename__="user"

    email = db.Column(db.String(45),primary_key = True)
    password = db.Column(db.String(60))
    birth_date = db.Column(db.Integer,nullable=True)
    sex = db.Column(db.Boolean,nullable=True)
    insulin_type = db.Column(db.String(45),nullable=True)

    #db.relationship("nombre de la tabla","backref = x->crea una variable en tabla hijo que se llame x y que referencie al padre para meal.user o user.meal","al eliminar usuario se borran hijos")
    meals = db.relationship("Meal",backref="user",cascade="all,delete")
    vitals = db.relationship("Vital",backref="user",cascade="all,delete")
    predictions = db.relationship("Prediction",backref="user",cascade="all,delete")

    def __init__(self, email, password, birth_date, sex, insulin_type):
        self.email = email
        self.password = password
        self.birth_date = birth_date
        self.sex = sex
        self.insulin_type = insulin_type

    def serialize(self):
        return {
            "email": self.email,
            "password": self.password,
            "birth_date": self.birth_date,
            "sex": self.sex,
            "insulin_type": self.insulin_type
            }

class Meal(db.Model):
    __tablename__="meal"

    meal_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    meal_time = db.Column(db.Integer)
    description = db.Column(db.String(300))

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))
    ingredients = db.relationship("Ingredient",backref="meal",cascade="all,delete")

class Ingredient(db.Model):
    __tablename__="ingredient"

    ingrediend_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    ingredient_name = db.Column(db.String(20))
    quantity = db.Column(db.Float)
    estim_carbs= db.Column(db.Float)
    estim_protein= db.Column(db.Float)
    estim_fats= db.Column(db.Float)

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))
    meal_meal_id=db.Column(db.Integer,db.ForeignKey("meal.meal_id"))

class Vital(db.Model):
    __tablename__="vital"

    vital_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    glucose_value = db.Column(db.Float)
    vital_time = db.Column(db.Integer)
    heart_rate = db.Column(db.Float,nullable=True)
    temperature = db.Column(db.Float,nullable=True)
    calories = db.Column(db.Float,nullable=True)
    diastolic = db.Column(db.Float,nullable=True)
    systolic = db.Column(db.Float,nullable=True)
    is_sleeping = db.Column(db.Boolean,nullable=True)

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

    def __init__(self, user_email, vital_time, glucose_value, heart_rate, temperature, calories, diastolic, systolic, is_sleeping):
        self.vital_time = vital_time
        self.user_email = user_email
        self.glucose_value = glucose_value
        self.heart_rate = heart_rate
        self.temperature = temperature
        self.calories = calories
        self.diastolic = diastolic
        self.systolic = systolic
        self.is_sleeping = is_sleeping

    def serialize(self):
        return {
            "vital_time": self.vital_time,
            "glucose_value": self.glucose_value,
            "heart_rate": self.heart_rate,
            "temperature": self.temperature,
            "calories": self.calories,
            "diastolic": self.diastolic,
            "systolic": self.systolic,
            "is_sleeping": self.is_sleeping
        }

class Prediction(db.Model):
    __tablename__="prediction"

    prediction_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    predict_time = db.Column(db.Integer)
    confidence_lvl = db.Column(db.Integer)
    forecast_time = db.Column(db.Integer)
    forecast = db.Column(db.String(300))

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))
