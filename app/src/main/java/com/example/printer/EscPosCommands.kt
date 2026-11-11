package com.example.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Comandos estándar ESC/POS para impresoras térmicas (58mm / 80mm).
 */
object EscPosCommands {
  // Inicialización
  val INIT = byteArrayOf(0x1B, 0x40) // ESC @

  // Alineación de texto
  val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
  val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
  val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2

  // Énfasis / Negrita
  val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
  val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0

  // Tamaño de fuente (GS ! n)
  val TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00) // 1x
  val TEXT_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x01)
  val TEXT_DOUBLE_WIDTH = byteArrayOf(0x1D, 0x21, 0x10)
  val TEXT_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // 2x ancho y alto

  // Subrayado
  val UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01)
  val UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00)

  // Salto de línea
  val LF = byteArrayOf(0x0A)

  // Alimentación de papel
  fun feedLines(lines: Int = 3): ByteArray {
    return byteArrayOf(0x1B, 0x64, lines.coerceIn(1, 10).toByte())
  }

  // Corte de papel (Corte total y parcial)
  val CUT_FULL = byteArrayOf(0x1D, 0x56, 0x41, 0x00) // GS V 65 0
  val CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0 (Alimenta y corta)
  val CUT_LEGACY = byteArrayOf(0x1D, 0x56, 0x00) // GS V 0

  // Zumbador
  val BEEP = byteArrayOf(0x1B, 0x42, 0x02, 0x02)

  // Codepage Latino / CP437 / ISO-8859-1
  val CODE_PAGE_CP437 = byteArrayOf(0x1B, 0x74, 0x00)
  val CODE_PAGE_WPC1252 = byteArrayOf(0x1B, 0x74, 0x10)
}

enum class EscAlign {
  LEFT,
  CENTER,
  RIGHT
}

/**
 * Generador fluido de secuencias binarias ESC/POS.
 */
class EscPosBuilder(val paperWidth: PaperWidth = PaperWidth.WIDTH_58MM) {
  private val out = ByteArrayOutputStream()
  private val charset: Charset = Charset.forName("CP437")

  val columns: Int get() = paperWidth.columns

  init {
    out.write(EscPosCommands.INIT)
    out.write(EscPosCommands.CODE_PAGE_CP437)
  }

  fun raw(bytes: ByteArray): EscPosBuilder {
    out.write(bytes)
    return this
  }

  fun align(align: EscAlign): EscPosBuilder {
    when (align) {
      EscAlign.LEFT -> out.write(EscPosCommands.ALIGN_LEFT)
      EscAlign.CENTER -> out.write(EscPosCommands.ALIGN_CENTER)
      EscAlign.RIGHT -> out.write(EscPosCommands.ALIGN_RIGHT)
    }
    return this
  }

  fun bold(enabled: Boolean): EscPosBuilder {
    out.write(if (enabled) EscPosCommands.BOLD_ON else EscPosCommands.BOLD_OFF)
    return this
  }

  fun doubleSize(enabled: Boolean): EscPosBuilder {
    out.write(if (enabled) EscPosCommands.TEXT_DOUBLE_SIZE else EscPosCommands.TEXT_NORMAL)
    return this
  }

  fun text(str: String): EscPosBuilder {
    val clean = sanitizeForThermal(str)
    out.write(clean.toByteArray(charset))
    return this
  }

  fun textLine(str: String = ""): EscPosBuilder {
    text(str)
    out.write(EscPosCommands.LF)
    return this
  }

  fun centerLine(str: String): EscPosBuilder {
    align(EscAlign.CENTER)
    textLine(str)
    align(EscAlign.LEFT)
    return this
  }

  fun divider(char: Char = '-'): EscPosBuilder {
    val line = char.toString().repeat(columns)
    align(EscAlign.LEFT)
    textLine(line)
    return this
  }

  fun row(left: String, right: String): EscPosBuilder {
    val cleanLeft = sanitizeForThermal(left)
    val cleanRight = sanitizeForThermal(right)
    val space = columns - cleanLeft.length - cleanRight.length
    val line = if (space > 0) {
      cleanLeft + " ".repeat(space) + cleanRight
    } else {
      cleanLeft.take(columns - cleanRight.length - 1) + " " + cleanRight
    }
    align(EscAlign.LEFT)
    textLine(line)
    return this
  }

  fun feed(lines: Int = 4): EscPosBuilder {
    out.write(EscPosCommands.feedLines(lines))
    return this
  }

  fun cut(): EscPosBuilder {
    feed(3)
    out.write(EscPosCommands.CUT_PARTIAL)
    return this
  }

  fun build(): ByteArray {
    return out.toByteArray()
  }

  private fun sanitizeForThermal(input: String): String {
    // Normalizar tildes y caracteres especiales para compatibilidad con impresoras térmicas
    return input
      .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
      .replace('Á', 'A').replace('É', 'E').replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U')
      .replace('ñ', 'n').replace('Ñ', 'N')
      .replace("¿", "").replace("¡", "")
  }
}
