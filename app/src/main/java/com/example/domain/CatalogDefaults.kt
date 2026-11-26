package com.example.domain

data class CatalogProduct(
    val name: String,
    val emoji: String,
    val categoryKey: String,
    val categoryLabel: String,
    val defaultUnit: String = "كغ"
)

data class QuickExpensePreset(
    val id: Long,
    val icon: String,
    val name: String,
    val amountCents: Long,
    val category: String
)

data class BudgetEnvelopeItem(
    val id: Long,
    val name: String,
    val amountCents: Long
)

object CatalogDefaults {
    val categoryTabs: List<Pair<String, String>> = listOf(
        "veg" to "خضر",
        "fruit" to "فواكه",
        "wmeat" to "لحوم بيضاء",
        "rmeat" to "لحوم حمراء",
        "fish" to "أسماك",
        "spices" to "توابل",
        "food" to "مواد غذائية"
    )

    val products: List<CatalogProduct> = listOf(
        // خضر
        CatalogProduct("بطاطا", "🥔", "veg", "خضر", "كغ"),
        CatalogProduct("طماطم", "🍅", "veg", "خضر", "كغ"),
        CatalogProduct("بصل", "🧅", "veg", "خضر", "كغ"),
        CatalogProduct("ثوم", "🧄", "veg", "خضر", "كغ"),
        CatalogProduct("جزر", "🥕", "veg", "خضر", "كغ"),
        CatalogProduct("قرعة (كوسة)", "🥒", "veg", "خضر", "كغ"),
        CatalogProduct("فلفل حلو", "🫑", "veg", "خضر", "كغ"),
        CatalogProduct("فلفل حار", "🌶️", "veg", "خضر", "كغ"),
        CatalogProduct("باذنجان", "🍆", "veg", "خضر", "كغ"),
        CatalogProduct("خيار", "🥒", "veg", "خضر", "كغ"),
        CatalogProduct("خس (سلاطة)", "🥬", "veg", "خضر", "حبة"),
        CatalogProduct("شفلور / بروكلي", "🥦", "veg", "خضر", "حبة"),
        CatalogProduct("ملفوف (كرنب)", "🥬", "veg", "خضر", "حبة"),
        CatalogProduct("جلبانة", "🫛", "veg", "خضر", "كغ"),
        CatalogProduct("معدنوس وقزبر", "🌿", "veg", "خضر", "حبة"),
        // فواكه
        CatalogProduct("تفاح", "🍎", "fruit", "فواكه", "كغ"),
        CatalogProduct("موز", "🍌", "fruit", "فواكه", "كغ"),
        CatalogProduct("برتقال", "🍊", "fruit", "فواكه", "كغ"),
        CatalogProduct("فراولة", "🍓", "fruit", "فواكه", "كغ"),
        CatalogProduct("رمان", "🍎", "fruit", "فواكه", "كغ"),
        CatalogProduct("ليمون (قارص)", "🍋", "fruit", "فواكه", "كغ"),
        CatalogProduct("تمر", "🌴", "fruit", "فواكه", "كغ"),
        CatalogProduct("عنب", "🍇", "fruit", "فواكه", "كغ"),
        CatalogProduct("دلاع (بطيخ)", "🍉", "fruit", "فواكه", "حبة"),
        // لحوم بيضاء
        CatalogProduct("دجاج كامل", "🍗", "wmeat", "لحوم بيضاء", "كغ"),
        CatalogProduct("صدر دجاج", "🍗", "wmeat", "لحوم بيضاء", "كغ"),
        CatalogProduct("أفخاذ دجاج", "🍗", "wmeat", "لحوم بيضاء", "كغ"),
        CatalogProduct("كبد دجاج", "🥩", "wmeat", "لحوم بيضاء", "كغ"),
        CatalogProduct("ديك رومي (داند)", "🦃", "wmeat", "لحوم بيضاء", "كغ"),
        // لحوم حمراء
        CatalogProduct("لحم بقري", "🥩", "rmeat", "لحوم حمراء", "كغ"),
        CatalogProduct("لحم هبرة", "🥩", "rmeat", "لحوم حمراء", "كغ"),
        CatalogProduct("لحم غنمي", "🍖", "rmeat", "لحوم حمراء", "كغ"),
        CatalogProduct("مرقاز / نقانق", "🌭", "rmeat", "لحوم حمراء", "كغ"),
        // أسماك
        CatalogProduct("سردين", "🐟", "fish", "أسماك", "كغ"),
        CatalogProduct("سمك دوراد/أبيض", "🐟", "fish", "أسماك", "كغ"),
        CatalogProduct("جمبري (كروفيت)", "🦐", "fish", "أسماك", "كغ"),
        // توابل
        CatalogProduct("فلفل أسود", "⚫", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("كمون", "🌿", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("راس الحانوت", "🌶️", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("كركم", "🟡", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("فلفل عكري (بابريكا)", "🌶️", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("زنجبيل", "🫚", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("قرفة", "🪵", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("زعفران", "🧡", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("قصبر (كزبرة يابسة)", "🌿", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("كروية", "🌱", "spices", "توابل وبهارات", "غ"),
        CatalogProduct("زعتر ورند", "🍃", "spices", "توابل وبهارات", "حزمة"),
        CatalogProduct("ثوم وبصل بودرة", "🧄", "spices", "توابل وبهارات", "علبة"),
        // مواد غذائية
        CatalogProduct("خبز", "🥖", "food", "مواد غذائية", "حبة"),
        CatalogProduct("حليب", "🥛", "food", "مواد غذائية", "لتر"),
        CatalogProduct("بيض", "🥚", "food", "مواد غذائية", "حبة"),
        CatalogProduct("سميد", "🌾", "food", "مواد غذائية", "كغ"),
        CatalogProduct("فرينة", "🥖", "food", "مواد غذائية", "كغ"),
        CatalogProduct("زيت المائدة", "🌻", "food", "مواد غذائية", "لتر"),
        CatalogProduct("زيت زيتون", "🫒", "food", "مواد غذائية", "لتر"),
        CatalogProduct("سكر", "🍬", "food", "مواد غذائية", "كغ"),
        CatalogProduct("قهوة", "☕", "food", "مواد غذائية", "علبة"),
        CatalogProduct("جبن (فرماج)", "🧀", "food", "مواد غذائية", "علبة"),
        CatalogProduct("زبدة", "🧈", "food", "مواد غذائية", "علبة"),
        CatalogProduct("كسكس", "🍲", "food", "مواد غذائية", "كغ"),
        CatalogProduct("أرز", "🍚", "food", "مواد غذائية", "كغ"),
        CatalogProduct("سباغيتي / مقرونة", "🍝", "food", "مواد غذائية", "علبة"),
        CatalogProduct("عدس", "🫘", "food", "مواد غذائية", "كغ"),
        CatalogProduct("لوبيا يابسة", "🫘", "food", "مواد غذائية", "كغ"),
        CatalogProduct("طماطم مصبرة", "🥫", "food", "مواد غذائية", "علبة")
    )

    val units = listOf("حبة", "كغ", "غ", "لتر", "مل", "علبة", "حزمة")

    val expenseCategories = listOf(
        "مواد غذائية", "خضر وفواكه", "لحوم وأسماك", "توابل وبهارات", "نقل ومواصلات",
        "فواتير", "صحة ودواء", "ملابس", "تعليم", "صيانة", "أخرى"
    )

    val standardBills = listOf(
        "فاتورة الغاز والكهرباء",
        "فاتورة الإنترنت",
        "فاتورة الكراء",
        "فاتورة الماء"
    )

    val rentUnits = listOf(
        "كراء سكن",
        "كراء محل",
        "كراء كراج",
        "كراء محل رقم 01",
        "كراء محل رقم 02"
    )

    val sewingStages = listOf(
        "جديدة", "قيد القص", "قيد الخياطة", "بانتظار التجربة", "جاهزة", "تم التسليم"
    )

    val defaultQuickExpenses = listOf(
        QuickExpensePreset(1L, "🥖", "خبز", 40_00L, "مواد غذائية"),
        QuickExpensePreset(2L, "🥛", "حليب", 120_00L, "مواد غذائية"),
        QuickExpensePreset(3L, "☕", "قهوة", 100_00L, "مواد غذائية"),
        QuickExpensePreset(4L, "🚕", "نقل", 200_00L, "نقل ومواصلات"),
        QuickExpensePreset(5L, "🥦", "خضر سريعة", 300_00L, "خضر وفواكه")
    )

    val defaultBudgetEnvelopes = listOf(
        BudgetEnvelopeItem(1L, "الكراء", 0L),
        BudgetEnvelopeItem(2L, "المصروف", 0L)
    )

    val seasonalOccasions = listOf(
        "رمضان" to "🌙",
        "عيد الفطر" to "🎁",
        "عيد الأضحى" to "🐑",
        "الدخول المدرسي" to "🎒",
        "أعراس ومناسبات" to "💐",
        "عطلة صيفية" to "☀️"
    )
}
