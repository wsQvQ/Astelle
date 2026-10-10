package com.astelle.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * 图片管道（⑫，拍板）：
 * 1. 相册选中的图**收进 app 私有目录** `files/images/<uuid>.jpg`，缩到 ~1600px / JPEG 85%
 *    （以「不明显变糊」为准，要调改 [MAX_DIM]/[QUALITY]）
 * 2. 正文只记**相对路径** `images/<uuid>.jpg` —— 不存 `content://`（临时授权，重启就废）
 * 3. 删笔记只删 **app 私有副本**，绝不动系统相册 —— 产品红线
 *
 * ⚠️ 硬件位图不给 compress（导出 PNG 时翻过车），解码一律要软件位图。
 */
object ImageStore {

    private const val DIR = "images"

    /** 把外部图片收进私有目录，返回正文里的相对路径；失败返回 null。
     *  [maxDim]/[quality] 由设置页的压缩档位供给（默认 = 标准档） */
    fun importImage(context: Context, uri: Uri, maxDim: Int = 1600, quality: Int = 85): String? {
        return try {
            val source = decode(context, uri) ?: return null
            val scaled = scaleDown(source, maxDim)
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            val name = "${UUID.randomUUID()}.jpg"
            val target = File(dir, name)
            target.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (scaled !== source) scaled.recycle()
            source.recycle()
            "$DIR/$name"
        } catch (_: Exception) {
            null
        }
    }

    /** 相对路径 → 私有目录里的文件；带路径穿越防护，越界返回 null */
    fun resolve(context: Context, relative: String): File? {
        if (!relative.startsWith("$DIR/")) return null
        val file = File(context.filesDir, relative)
        val root = File(context.filesDir, DIR).canonicalPath
        return if (file.canonicalPath.startsWith(root + File.separator)) file else null
    }

    /** 删笔记连带清图：**只删 app 私有副本**，绝不动系统相册（产品红线） */
    fun deleteImages(context: Context, relativePaths: Collection<String>) {
        relativePaths.forEach { path -> resolve(context, path)?.delete() }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder 会按 EXIF 转正照片方向（BitmapFactory 不会，横拍会躺下）
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / max
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
