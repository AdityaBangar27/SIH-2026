import matplotlib.pyplot as plt
import pandas as pd
from sklearn.linear_model import LinearRegression

data = {"Experience": [1, 2, 3, 4, 5], "Salary": [30000, 35000, 40000, 45000, 50000]}
df = pd.DataFrame(data)

x = df[["Experience"]]
y = df[["Salary"]]

model = LinearRegression()
model.fit(x, y)

y_pred = model.predict([[6]])
print("Predicted y =", y_pred)

plt.scatter(x, y)
plt.plot(x, model.predict(x))
plt.scatter(6, y_pred, color="red")
plt.xlabel("Experience")
plt.ylabel("Salary")

plt.show()