# Generates core/database/src/main/assets/seed/recipes.json from the recipe list below.
# Usage: python3 tools/seed/recipes.py <repo root>. Bump "version" when changing recipes.
import json, sys
root = sys.argv[1]
foods = {f["id"]: f for f in json.load(open(f"{root}/core/database/src/main/assets/seed/foods.json"))["foods"]}
B, L, D, S = "BREAKFAST", "LUNCH", "DINNER", "SNACK"
R = []
def r(id, name, servings, meals, ingredients, steps):
    R.append({"id": "builtin-" + id, "name": name, "servings": servings, "meals": meals,
              "ingredients": [{"food": "seed-" + f, "grams": g} for f, g in ingredients], "steps": steps})

# Breakfasts
r("overnight-oats", "Overnight oats with berries", 1, [B], [("oats", 50), ("milk-semi-skimmed", 200), ("frozen-berries", 80), ("chia-seeds", 10)], ["Stir everything together in a jar.", "Refrigerate overnight; eat cold or warmed."])
r("protein-oats", "Protein porridge", 1, [B], [("oats", 60), ("milk-semi-skimmed", 250), ("whey-protein", 30), ("banana", 60)], ["Simmer oats in milk for 4–5 minutes.", "Off the heat, stir in the whey; top with sliced banana."])
r("skyr-bowl", "Skyr bowl with granola", 1, [B, S], [("skyr", 200), ("granola", 40), ("frozen-berries", 80), ("honey", 10)], ["Spoon skyr into a bowl.", "Top with berries, granola and a drizzle of honey."])
r("greek-yogurt-parfait", "Greek yogurt parfait", 1, [B, S], [("greek-yogurt-nonfat", 250), ("blueberries", 100), ("walnuts", 15), ("honey", 10)], ["Layer yogurt and blueberries in a glass.", "Finish with chopped walnuts and honey."])
r("veggie-omelette", "Vegetable omelette", 1, [B], [("egg-whole", 150), ("bell-pepper", 60), ("spinach", 30), ("onion", 40), ("olive-oil", 5)], ["Soften onion and pepper in the oil.", "Add spinach, then the beaten eggs; cook until just set and fold."])
r("egg-white-scramble", "Egg-white scramble on toast", 1, [B], [("egg-white", 200), ("egg-whole", 50), ("whole-wheat-bread", 64), ("tomato", 100)], ["Scramble the eggs and whites over low heat.", "Serve on toast with sliced tomato."])
r("shakshuka", "Shakshuka", 2, [B, L], [("canned-tomatoes", 400), ("onion", 110), ("bell-pepper", 120), ("garlic", 6), ("egg-whole", 200), ("olive-oil", 10), ("whole-wheat-bread", 64)], ["Soften onion, pepper and garlic in the oil.", "Add tomatoes and simmer 10 minutes.", "Make wells, crack in the eggs, cover until set. Serve with bread."])
r("banana-pb-toast", "Peanut butter banana toast", 1, [B, S], [("whole-wheat-bread", 64), ("peanut-butter", 20), ("banana", 100)], ["Toast the bread.", "Spread peanut butter and top with banana slices."])
r("cottage-cheese-toast", "Cottage cheese and tomato toast", 1, [B, S], [("whole-wheat-bread", 64), ("cottage-cheese", 150), ("tomato", 100), ("olive-oil", 5)], ["Toast the bread and spread with cottage cheese.", "Top with tomato and a little olive oil."])
r("breakfast-wrap", "Egg and spinach breakfast wrap", 1, [B], [("whole-wheat-tortilla", 45), ("egg-whole", 100), ("egg-white", 66), ("spinach", 30), ("avocado", 50)], ["Scramble the eggs and whites with the spinach.", "Fill the tortilla with eggs and sliced avocado; roll."])
r("berry-smoothie", "Berry protein smoothie", 1, [B, S], [("frozen-berries", 150), ("banana", 100), ("milk-semi-skimmed", 250), ("whey-protein", 30)], ["Blend until smooth."])
r("dates-oats", "Date and walnut oatmeal", 1, [B], [("oats", 60), ("milk-semi-skimmed", 250), ("dates", 24), ("walnuts", 15)], ["Simmer oats in milk.", "Top with chopped date and walnuts."])

# Lunches and dinners
r("chicken-rice-bowl", "Chicken, rice and broccoli bowl", 1, [L, D], [("chicken-breast-cooked", 150), ("white-rice-cooked", 200), ("broccoli-cooked", 150), ("olive-oil", 5)], ["Slice the chicken.", "Serve over rice with steamed broccoli and a drizzle of oil."])
r("chicken-wrap", "Chicken and hummus wrap", 1, [L], [("whole-wheat-tortilla", 45), ("chicken-breast-cooked", 120), ("hummus", 30), ("lettuce", 40), ("tomato", 60)], ["Spread hummus on the tortilla.", "Add chicken, lettuce and tomato; roll tightly."])
r("tuna-salad", "Tuna and chickpea salad", 1, [L], [("tuna-canned-water", 112), ("chickpeas-cooked", 120), ("cucumber", 100), ("tomato", 100), ("olive-oil", 10), ("lemon-juice", 15)], ["Toss everything together.", "Season with salt and pepper."])
r("lentil-soup", "Red lentil soup", 4, [L, D], [("red-lentils-dry", 250), ("onion", 220), ("carrot", 120), ("garlic", 9), ("canned-tomatoes", 400), ("olive-oil", 20)], ["Soften onion, carrot and garlic in the oil.", "Add lentils, tomatoes and 1.2 L water; simmer 20 minutes.", "Blend until smooth and season."])
r("salmon-potatoes", "Salmon with potatoes and greens", 1, [D], [("salmon-cooked", 150), ("potato-boiled", 250), ("broccoli-cooked", 120), ("lemon-juice", 15)], ["Bake the salmon at 200 °C for 12–15 minutes.", "Serve with boiled potatoes, broccoli and lemon."])
r("beef-chili", "Lean beef and bean chili", 4, [L, D], [("lean-beef-mince-cooked", 500), ("black-beans-cooked", 400), ("canned-tomatoes", 800), ("onion", 220), ("bell-pepper", 240), ("garlic", 9), ("white-rice-cooked", 600)], ["Brown the mince with onion, pepper and garlic.", "Add tomatoes and beans; simmer 25 minutes with chili spices.", "Serve over rice."])
r("turkey-bolognese", "Turkey bolognese", 4, [D], [("turkey-mince-cooked", 500), ("canned-tomatoes", 800), ("onion", 220), ("carrot", 120), ("garlic", 9), ("pasta-cooked", 800)], ["Brown the turkey with onion, carrot and garlic.", "Add tomatoes and simmer 20 minutes.", "Serve over pasta."])
r("chicken-stir-fry", "Chicken vegetable stir-fry", 2, [D], [("chicken-breast-cooked", 300), ("frozen-mixed-veg", 300), ("bell-pepper", 120), ("olive-oil", 15), ("brown-rice-cooked", 390)], ["Stir-fry the vegetables in the oil.", "Add the chicken and toss with soy sauce and ginger.", "Serve over brown rice."])
r("shrimp-couscous", "Lemon garlic shrimp couscous", 2, [D], [("shrimp-cooked", 250), ("couscous-cooked", 320), ("zucchini", 200), ("garlic", 6), ("olive-oil", 15), ("lemon-juice", 30)], ["Sauté zucchini and garlic in the oil.", "Add shrimp to warm through; finish with lemon.", "Serve over couscous."])
r("cod-tray-bake", "Cod and vegetable tray bake", 2, [D], [("cod-cooked", 360), ("potato-boiled", 400), ("bell-pepper", 240), ("zucchini", 200), ("olive-oil", 15)], ["Roast potatoes and vegetables at 210 °C for 25 minutes.", "Add the cod and roast 10 minutes more."])
r("chickpea-curry", "Chickpea and spinach curry", 3, [L, D], [("chickpeas-cooked", 480), ("canned-tomatoes", 400), ("spinach", 120), ("onion", 220), ("garlic", 9), ("olive-oil", 15), ("white-rice-cooked", 480)], ["Soften onion and garlic; add curry spices.", "Add tomatoes and chickpeas; simmer 15 minutes; wilt in the spinach.", "Serve over rice."])
r("tofu-stir-fry", "Tofu and mushroom stir-fry", 2, [D], [("tofu-firm", 300), ("mushrooms", 200), ("frozen-mixed-veg", 250), ("olive-oil", 15), ("brown-rice-cooked", 390)], ["Pan-fry cubed tofu until golden.", "Add vegetables and stir-fry; season with soy sauce.", "Serve with rice."])
r("steak-sweet-potato", "Steak with sweet potato", 1, [D], [("beef-sirloin-cooked", 170), ("sweet-potato-baked", 200), ("spinach", 60), ("olive-oil", 5)], ["Sear the steak to your liking; rest 5 minutes.", "Serve with baked sweet potato and wilted spinach."])
r("chicken-shawarma-bowl", "Chicken shawarma bowl", 1, [L, D], [("chicken-thigh-cooked", 150), ("bulgur-cooked", 180), ("cucumber", 100), ("tomato", 100), ("tahini", 15), ("lemon-juice", 15)], ["Season chicken with shawarma spices and grill.", "Serve over bulgur with cucumber, tomato and lemon-tahini sauce."])
r("tuna-pasta", "Tuna tomato pasta", 2, [L, D], [("tuna-canned-water", 224), ("pasta-cooked", 400), ("canned-tomatoes", 400), ("garlic", 6), ("olive-oil", 10)], ["Simmer tomatoes with garlic and oil for 10 minutes.", "Stir in tuna and the cooked pasta."])
r("beef-pita", "Beef kofta pita", 2, [L, D], [("lean-beef-mince-cooked", 250), ("pita-bread", 128), ("cucumber", 100), ("greek-yogurt-nonfat", 100), ("onion", 55)], ["Mix mince with grated onion and spices; shape and grill.", "Serve in warm pita with cucumber and yogurt sauce."])
r("salmon-quinoa", "Salmon quinoa salad", 1, [L], [("salmon-cooked", 120), ("quinoa-cooked", 185), ("cucumber", 80), ("spinach", 30), ("lemon-juice", 15), ("olive-oil", 5)], ["Flake the salmon over quinoa and greens.", "Dress with lemon and oil."])
r("egg-fried-rice", "Egg fried rice with vegetables", 2, [L, D], [("egg-whole", 150), ("brown-rice-cooked", 390), ("frozen-mixed-veg", 250), ("olive-oil", 15)], ["Stir-fry vegetables in the oil.", "Add rice, then push aside and scramble the eggs; toss together with soy sauce."])
r("lentil-bolognese", "Lentil bolognese", 3, [D], [("lentils-cooked", 400), ("canned-tomatoes", 400), ("onion", 110), ("carrot", 120), ("mushrooms", 150), ("pasta-cooked", 600), ("olive-oil", 15)], ["Soften onion, carrot and mushrooms.", "Add tomatoes and lentils; simmer 15 minutes.", "Serve over pasta."])
r("chicken-sweet-potato", "Chicken and sweet potato traybake", 2, [D], [("chicken-breast-cooked", 300), ("sweet-potato-baked", 400), ("bell-pepper", 240), ("olive-oil", 15)], ["Roast sweet potato and peppers at 200 °C for 25 minutes.", "Add sliced chicken for the last 10 minutes."])
r("turkey-wrap", "Turkey and avocado wrap", 1, [L], [("whole-wheat-tortilla", 45), ("turkey-mince-cooked", 120), ("avocado", 50), ("lettuce", 40), ("tomato", 60)], ["Warm the turkey with taco spices.", "Fill the tortilla with turkey, avocado, lettuce and tomato."])
r("bean-quinoa-bowl", "Black bean quinoa bowl", 1, [L], [("black-beans-cooked", 150), ("quinoa-cooked", 185), ("bell-pepper", 80), ("avocado", 50), ("lemon-juice", 15)], ["Combine beans, quinoa and pepper.", "Top with avocado and lemon juice."])
r("fish-tacos", "Cod tacos", 2, [D], [("cod-cooked", 300), ("whole-wheat-tortilla", 180), ("lettuce", 80), ("greek-yogurt-nonfat", 100), ("lemon-juice", 15)], ["Pan-cook the cod with paprika and flake it.", "Serve in tortillas with lettuce and lemon yogurt."])
r("big-chicken-salad", "Big chicken salad", 1, [L], [("chicken-breast-cooked", 150), ("lettuce", 100), ("cucumber", 100), ("tomato", 100), ("chickpeas-cooked", 80), ("olive-oil", 10)], ["Slice the chicken.", "Toss with the vegetables and chickpeas; dress with oil and lemon."])
r("mass-gainer-bowl", "Beef, rice and egg power bowl", 1, [L, D], [("lean-beef-mince-cooked", 200), ("white-rice-cooked", 300), ("egg-whole", 50), ("avocado", 70), ("frozen-mixed-veg", 150)], ["Warm the mince and vegetables.", "Serve over rice with a fried egg and avocado."])

# Snacks
r("hummus-veg", "Hummus with vegetable sticks", 1, [S], [("hummus", 60), ("carrot", 100), ("cucumber", 100), ("bell-pepper", 60)], ["Cut vegetables into sticks and dip."])
r("apple-pb", "Apple with peanut butter", 1, [S], [("apple", 180), ("peanut-butter", 20)], ["Slice the apple and serve with peanut butter."])
r("cottage-berries", "Cottage cheese with berries", 1, [S], [("cottage-cheese", 200), ("frozen-berries", 100)], ["Top cottage cheese with thawed berries."])
r("protein-shake", "Protein shake with milk", 1, [S], [("whey-protein", 30), ("milk-semi-skimmed", 300)], ["Shake together."])
r("trail-mix", "Dates and almonds", 1, [S], [("dates", 48), ("almonds", 28)], ["Pack together for a quick snack."])
r("yogurt-honey", "Greek yogurt with honey and walnuts", 1, [S], [("greek-yogurt-nonfat", 170), ("honey", 10), ("walnuts", 15)], ["Top yogurt with honey and walnuts."])
r("egg-snack", "Boiled eggs and cucumber", 1, [S], [("egg-whole", 100), ("cucumber", 150)], ["Boil the eggs 9 minutes; serve with cucumber slices."])
r("chia-pudding", "Chia pudding", 1, [S, B], [("chia-seeds", 30), ("milk-semi-skimmed", 200), ("frozen-berries", 60)], ["Stir chia into milk; rest at least 2 hours.", "Top with berries."])
r("tuna-toast", "Tuna on toast", 1, [S], [("tuna-canned-water", 112), ("greek-yogurt-nonfat", 30), ("whole-wheat-bread", 32)], ["Mix tuna with yogurt and pepper.", "Spread on toast."])
r("banana-shake", "Banana peanut shake", 1, [S, B], [("banana", 120), ("milk-semi-skimmed", 250), ("peanut-butter", 16), ("oats", 30)], ["Blend until smooth."])

def nutrients(rec):
    t = {k: 0.0 for k in ("kcal", "protein")}
    for i in rec["ingredients"]:
        f = foods[i["food"]]
        t["kcal"] += f["kcal"] * i["grams"] / 100
        t["protein"] += f["protein"] * i["grams"] / 100
    return {k: v / rec["servings"] for k, v in t.items()}

missing = [i["food"] for rec in R for i in rec["ingredients"] if i["food"] not in foods]
assert not missing, missing
ids = [rec["id"] for rec in R]
assert len(ids) == len(set(ids))
json.dump({"version": 1, "recipes": R}, open(f"{root}/core/database/src/main/assets/seed/recipes.json", "w"), indent=1, ensure_ascii=False)
for rec in R:
    n = nutrients(rec)
    print(f'{rec["name"][:34]:34} {round(n["kcal"]):5} kcal {round(n["protein"]):3} g P  {",".join(rec["meals"])}')
print(len(R))
