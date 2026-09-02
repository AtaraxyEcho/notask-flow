package com.notaskflow.data.attachment.dto

import com.notaskflow.domain.model.NoteAttachment
import com.notaskflow.domain.model.NoteAttachmentUpload
import com.notaskflow.domain.model.TaskAttachment
import com.notaskflow.domain.model.TaskAttachmentUpload
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttachmentDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "fileName") val fileName: String?,
    @param:Json(name = "fileSize") val fileSize: Long?,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "downloadUrl") val downloadUrl: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

@JsonClass(generateAdapter = true)
data class AttachmentBindRequestDto(
    @param:Json(name = "attachmentId") val attachmentId: Long,
    @param:Json(name = "businessType") val businessType: String,
    @param:Json(name = "businessId") val businessId: Long,
    @param:Json(name = "referenceKey") val referenceKey: String?
)

@JsonClass(generateAdapter = true)
data class AttachmentUnbindRequestDto(
    @param:Json(name = "businessType") val businessType: String,
    @param:Json(name = "businessId") val businessId: Long,
    @param:Json(name = "referenceKey") val referenceKey: String?
)

fun AttachmentDto.toNoteDomain(): NoteAttachment {
    return NoteAttachment(
        id = id,
        fileName = fileName.orEmpty(),
        fileSize = fileSize ?: 0L,
        mimeType = mimeType,
        downloadUrl = downloadUrl,
        gmtCreate = gmtCreate
    )
}

fun AttachmentDto.toTaskDomain(): TaskAttachment {
    return TaskAttachment(
        id = id,
        fileName = fileName.orEmpty(),
        fileSize = fileSize ?: 0L,
        mimeType = mimeType,
        downloadUrl = downloadUrl,
        gmtCreate = gmtCreate
    )
}

fun NoteAttachmentUpload.toBindRequestDto(
    attachmentId: Long,
    noteId: Long
): AttachmentBindRequestDto {
    return AttachmentBindRequestDto(
        attachmentId = attachmentId,
        businessType = NOTE_BUSINESS_TYPE,
        businessId = noteId,
        referenceKey = referenceKey
    )
}

fun TaskAttachmentUpload.toTaskBindRequestDto(
    attachmentId: Long,
    taskId: Long
): AttachmentBindRequestDto {
    return AttachmentBindRequestDto(
        attachmentId = attachmentId,
        businessType = TASK_BUSINESS_TYPE,
        businessId = taskId,
        referenceKey = referenceKey
    )
}

fun noteAttachmentUnbindRequestDto(
    noteId: Long,
    referenceKey: String?
): AttachmentUnbindRequestDto {
    return AttachmentUnbindRequestDto(
        businessType = NOTE_BUSINESS_TYPE,
        businessId = noteId,
        referenceKey = referenceKey
    )
}

fun taskAttachmentUnbindRequestDto(
    taskId: Long,
    referenceKey: String?
): AttachmentUnbindRequestDto {
    return AttachmentUnbindRequestDto(
        businessType = TASK_BUSINESS_TYPE,
        businessId = taskId,
        referenceKey = referenceKey
    )
}

private const val NOTE_BUSINESS_TYPE = "NOTE"
private const val TASK_BUSINESS_TYPE = "TASK"
