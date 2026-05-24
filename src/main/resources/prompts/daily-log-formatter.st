Extract the user's meals by meal type. Output ONLY in this format, nothing else:
Kahvaltı: [foods]. Öğle: [foods]. Akşam: [foods]. Atıştırmalık: [foods].

Rules:
- Format: [AMOUNT] [UNIT] [FOOD] — example: 1 fincan cay, 2 dilim ekmek, 1 porsiyon pilav
- Units: fincan, bardak, kase, tabak, porsiyon, dilim, adet, kasik
- Morning/kahvalti -> Kahvalti, midday -> Ogle, evening -> Aksam, night/before bed/snack -> Atistirmalik
- If a meal was not eaten at all, DO NOT write that meal (not even a dot, skip entirely)
- If nothing was eaten, write only a dash: -
- Do NOT write or count water
- If a food is mentioned but amount is unclear, guess a reasonable unit
- If no concrete food name is mentioned (only "kahvalti yaptim", "bir seyler yedik", "atistirdim"), DO NOT write that meal
- No explanations, comments, or greetings