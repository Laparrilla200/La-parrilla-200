package com.example.printer

enum class PaperWidth(
  val label: String,
  val mmWidth: Int,
  val columns: Int,
  val printWidthMm: Int
) {
  WIDTH_58MM("58mm (Estándar/Portátil)", 58, 32, 48),
  WIDTH_80MM("80mm (Punto de Venta Grande)", 80, 48, 72);

  companion object {
    fun fromWidth(widthMm: Int): PaperWidth {
      return if (widthMm >= 70) WIDTH_80MM else WIDTH_58MM
    }
  }
}
