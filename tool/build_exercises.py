#!/usr/bin/env python3
"""Собирает справочник упражнений для приложения из free-exercise-db.

Набор (общественное достояние) лежит в `exercise-db-src/`, сюда попадают только
имя, перевод, мышцы и идентификатор картинки — сами картинки приложение тянет с CDN.

    python3 tool/build_exercises.py
"""

import json
import os
import sys

SOURCE = "exercise-db-src/exercises.json"
TARGET = "app/src/main/assets/exercises.json"

MUSCLE = {
    "quadriceps": "Quads", "shoulders": "Shoulders", "abdominals": "Core", "chest": "Chest",
    "hamstrings": "Hamstrings", "triceps": "Triceps", "biceps": "Biceps", "lats": "Lats",
    "middle back": "UpperBack", "lower back": "UpperBack", "calves": "Calves",
    "glutes": "Glutes", "traps": "Traps", "forearms": "Biceps", "neck": "Traps",
    "adductors": "Quads", "abductors": "Glutes",
}

# Название собирается из движения, положения и снаряда — так же, как его называют в зале.
MOVES = [
    ("bench press", "Жим лёжа"), ("shoulder press", "Жим на плечи"),
    ("military press", "Армейский жим"), ("leg press", "Жим ногами"),
    ("chest press", "Жим от груди"), ("overhead press", "Жим над головой"),
    ("floor press", "Жим с пола"), ("push press", "Швунг"), ("press", "Жим"),
    ("preacher curl", "Сгибания на скамье Скотта"), ("hammer curl", "Молотковые сгибания"),
    ("leg curl", "Сгибания ног"), ("curl", "Сгибания на бицепс"),
    ("lat pulldown", "Тяга верхнего блока"), ("pulldown", "Тяга верхнего блока"),
    ("pull apart", "Разведение резины"), ("pull through", "Протяжка"), ("pullover", "Пуловер"),
    ("deadlift", "Становая тяга"), ("upright row", "Тяга к подбородку"), ("row", "Тяга"),
    ("squat", "Приседания"), ("lunge", "Выпады"), ("step up", "Зашагивания"),
    ("step-up", "Зашагивания"), ("calf raise", "Подъёмы на носки"),
    ("lateral raise", "Махи в стороны"), ("front raise", "Подъёмы перед собой"),
    ("leg raise", "Подъёмы ног"), ("glute bridge", "Ягодичный мост"),
    ("hip thrust", "Ягодичный мост"), ("bridge", "Мост"), ("raise", "Подъёмы"),
    ("leg extension", "Разгибания ног"), ("extension", "Разгибания"),
    ("pushdown", "Разгибания на блоке"), ("push down", "Разгибания на блоке"),
    ("kickback", "Разгибания в наклоне"), ("skullcrusher", "Французский жим"),
    ("skull crusher", "Французский жим"), ("flye", "Разведения"), ("fly", "Разведения"),
    ("crunch", "Скручивания"), ("sit-up", "Подъёмы корпуса"), ("situp", "Подъёмы корпуса"),
    ("shrug", "Шраги"), ("dip", "Отжимания на брусьях"), ("pull-up", "Подтягивания"),
    ("pullup", "Подтягивания"), ("chin-up", "Подтягивания обратным хватом"),
    ("push-up", "Отжимания"), ("pushup", "Отжимания"), ("plank", "Планка"),
    ("good morning", "Наклоны со штангой"), ("face pull", "Тяга к лицу"),
    ("thruster", "Трастеры"), ("side bend", "Наклоны в сторону"),
    ("rollout", "Выкат ролика"), ("roll-out", "Выкат ролика"), ("ab roller", "Выкат ролика"),
    ("swing", "Махи гирей"), ("clean and jerk", "Взятие на грудь и толчок"),
    ("clean", "Взятие на грудь"), ("snatch", "Рывок"), ("jerk", "Толчок"),
    ("farmer", "Прогулка фермера"), ("carry", "Прогулка с весом"), ("drag", "Протяжка саней"),
    ("sled", "Сани"), ("woodchop", "Дровосек"), ("chop", "Дровосек"),
    ("russian twist", "Русские скручивания"), ("twist", "Повороты корпуса"),
    ("mountain climber", "Скалолаз"), ("burpee", "Бёрпи"),
    ("jumping jack", "Прыжки со сменой ног"), ("box jump", "Запрыгивания на тумбу"),
    ("jump", "Прыжки"), ("sprint", "Спринт"), ("crawl", "Ползание"),
    ("circles", "Круги руками"), ("superman", "Лодочка"), ("bird dog", "Птица-собака"),
    ("windmill", "Мельница"), ("turkish get-up", "Турецкий подъём"),
    ("get-up", "Турецкий подъём"), ("hyperextension", "Гиперэкстензия"),
    ("back extension", "Гиперэкстензия"), ("stretch", "Растяжка"),
    ("smr", "Прокатка на ролике"), ("hold", "Удержание"), ("walk", "Ходьба"),
    ("run", "Бег"), ("bike", "Велотренажёр"), ("rope", "Канаты"), ("throw", "Бросок"),
    ("slam", "Бросок мяча вниз"), ("scissor", "Ножницы"), ("hang", "Вис"),
    ("rotation", "Вращения"), ("adduction", "Сведение ног"), ("abduction", "Разведение ног"),
    ("kick", "Махи ногой"),
]

POSITION = [
    ("seated", "сидя"), ("standing", "стоя"), ("lying", "лёжа"), ("incline", "на наклонной"),
    ("decline", "на наклонной вниз"), ("bent over", "в наклоне"), ("bent-over", "в наклоне"),
    ("kneeling", "с колен"), ("one-arm", "одной рукой"), ("one arm", "одной рукой"),
    ("single-arm", "одной рукой"), ("single leg", "на одной ноге"),
    ("one-legged", "на одной ноге"), ("close-grip", "узким хватом"),
    ("wide-grip", "широким хватом"), ("wide grip", "широким хватом"),
    ("reverse grip", "обратным хватом"), ("romanian", "румынская"),
    ("stiff-legged", "на прямых ногах"), ("stiff leg", "на прямых ногах"),
    ("smith", "в Смите"), ("hack", "гакк"), ("front", "фронтальные"),
    ("overhead", "над головой"), ("side", "боковые"), ("bulgarian", "болгарские"),
    ("split", "сплит"), ("sumo", "сумо"), ("reverse", "обратные"),
]

GEAR = {
    "barbell": "со штангой", "dumbbell": "с гантелями", "cable": "на блоке",
    "machine": "в тренажёре", "e-z curl bar": "с EZ-грифом", "kettlebells": "с гирей",
    "bands": "с резиной", "medicine ball": "с медболом", "exercise ball": "на фитболе",
    "foam roll": "на ролике", "body only": "", "other": "", None: "",
}


# Слова, по которым видно, что снаряд уже назван: второй раз его дописывать не надо.
GEAR_WORDS = ("блок", "смит", "тренажёр", "штанг", "гантел", "гир", "резин", "ролик", "мяч", "медбол")


def translate(name, equipment):
    """Русское название, когда движение узнаётся. Иначе английское остаётся как есть."""
    low = name.lower().replace("/", " ")
    move = next((ru for en, ru in MOVES if en in low), None)
    if move is None:
        return None
    parts = [move]
    for word, russian in POSITION:
        if word not in low or russian in parts:
            continue
        if russian.lower() in " ".join(parts).lower():
            continue
        # «Жим на плечи над головой» — над головой их и жмут.
        if russian == "над головой" and "плеч" in move.lower():
            continue
        parts.append(russian)
    gear = GEAR.get(equipment, "")
    said = " ".join(parts).lower()
    if gear and not any(word in said for word in GEAR_WORDS):
        parts.append(gear)
    return " ".join(parts[:4])


def main():
    if not os.path.exists(SOURCE):
        print(f"нет исходника: {SOURCE}", file=sys.stderr)
        return 1
    source = json.load(open(SOURCE, encoding="utf-8"))
    out = []
    for item in source:
        primary = (item.get("primaryMuscles") or [None])[0]
        target = MUSCLE.get(primary)
        if target is None:
            continue
        secondary = []
        for muscle in item.get("secondaryMuscles", []):
            group = MUSCLE.get(muscle)
            if group and group != target and group not in secondary:
                secondary.append(group)
        illustration = item.get("id") if item.get("images") else None
        out.append({
            "name": item["name"],
            "nameRu": translate(item["name"], item.get("equipment")),
            "target": target,
            "secondary": secondary,
            "illustration": illustration,
            "frames": len(item.get("images") or []),
        })
    os.makedirs(os.path.dirname(TARGET), exist_ok=True)
    with open(TARGET, "w", encoding="utf-8") as file:
        json.dump(out, file, ensure_ascii=False, separators=(",", ":"))
    translated = sum(1 for e in out if e["nameRu"])
    print(f"упражнений: {len(out)}, с русским названием: {translated}")
    print(f"размер: {os.path.getsize(TARGET) // 1024} КБ → {TARGET}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
