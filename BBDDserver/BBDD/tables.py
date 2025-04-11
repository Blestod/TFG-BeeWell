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

        user_variable_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
        change_date_time = db.Column(db.Integer)
        height = db.Column(db.Float)
        weight = db.Column(db.Float)

        user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

        def __init__(self, user_email, change_date_time, height, weight, insulin_type):
            self.user_email = user_email
            self.change_date_time = change_date_time
            self.height = height
            self.weight = weight

        def serialize(self):
            return {
                "user_variable_id": self.user_variable_id,
                "change_date_time": self.change_date_time,
                "height": self.height,
                "weight": self.weight
            }

class InsulinInjected(db.Model):
    __tablename__ = "insulin_injected"

    injected_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    in_time = db.Column(db.Integer, nullable=False)
    in_units = db.Column(db.Float, nullable=False)
    insulin_type = db.Column(db.String(45), nullable=False)

    user_email = db.Column(db.String(45), db.ForeignKey("user.email"), nullable=False)

    def __init__(self, user_email, in_time, in_units, insulin_type):
        self.user_email = user_email
        self.in_time = in_time
        self.in_units = in_units
        self.insulin_type = insulin_type

    def serialize(self):
        return {
            "injected_id": self.injected_id,
            "in_time": self.in_time,
            "in_units": self.in_units,
            "insulin_type": self.insulin_type,
            "user_email": self.user_email
        }

class Meal(db.Model):
    __tablename__ = "meal"

    meal_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    meal_time = db.Column(db.Integer)
    description = db.Column(db.String(300))

    user_email = db.Column(db.String(45), db.ForeignKey("user.email"))

    # Relación con ingredientes
    ingredients = db.relationship("MealIngredient", back_populates="meal", cascade="all, delete")


class Ingredient(db.Model):
    __tablename__ = "ingredient"

    ingredient_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    ingredient_name = db.Column(db.String(255), unique=True, nullable=False)
    estim_carbs = db.Column(db.Float, nullable=True)
    estim_protein = db.Column(db.Float, nullable=True)
    estim_fats = db.Column(db.Float, nullable=True)
    i_g = db.Column(db.Float, nullable=True)

    # Relación inversa con MealIngredient
    meals = db.relationship("MealIngredient", back_populates="ingredient")


class MealIngredient(db.Model):
    __tablename__ = "meal_ingredient"

    meal_meal_id = db.Column(db.Integer, db.ForeignKey("meal.meal_id"), primary_key=True)
    ingredient_ingredient_id = db.Column(db.Integer, db.ForeignKey("ingredient.ingredient_id"), primary_key=True)

    quantity = db.Column(db.Float, nullable=False)
    unit = db.Column(db.String(20), nullable=False)

    # Relaciones inversas
    meal = db.relationship("Meal", back_populates="ingredients")
    ingredient = db.relationship("Ingredient", back_populates="meals")


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
