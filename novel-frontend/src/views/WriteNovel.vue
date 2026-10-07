<template>
  <div class="write-wrapper">
    <header class="header">
      <div class="header-left">
        <span class="brand">📚 夏辰义的书城</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="$router.push('/')">← 返回书城</el-button>
      </div>
    </header>

    <div class="container">
      <el-card class="form-card">
        <template #header>
          <h2>✍️ 发布新书</h2>
        </template>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          style="max-width: 600px; margin: 0 auto;"
        >
          <el-form-item label="书名" prop="title">
            <el-input v-model="form.title" placeholder="请输入小说名称" />
          </el-form-item>

          <el-form-item label="简介" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="4"
              placeholder="请简要介绍你的小说"
            />
          </el-form-item>

          <el-form-item label="分类" prop="category">
            <el-input v-model="form.category" placeholder="如：都市、玄幻、言情" />
          </el-form-item>

          <el-form-item label="封面">
            <el-upload
              class="avatar-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-preview" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="tip">支持 jpg/png 格式，建议 200×260 像素</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitForm">
              立即发布
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createNovel, uploadCover } from '../api/novel'

const router = useRouter()
const formRef = ref(null)

const form = reactive({
  title: '',
  summary: '',
  category: '',
  coverUrl: ''   // 上传成功后存储图片URL
})

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入简介', trigger: 'blur' }]
}

const submitting = ref(false)

// 上传接口地址（直接使用后端 /upload，会经过代理）
const uploadAction = '/api/upload'
// 上传时携带 Token
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response, file) => {
  // 注意：后端返回 Result<String>，实际数据在 response.data 中
  if (response.code === 200) {
    form.coverUrl = response.data   // 图片URL
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 上传失败
const handleUploadError = (error) => {
  ElMessage.error('上传失败，请重试')
  console.error(error)
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      // 构造请求体（根据后端 NovleDTO 字段调整）
      const payload = {
        title: form.title,
        summary: form.summary,
        coverUrl: form.coverUrl,
        category: form.category || '未分类'
      }
      const res = await createNovel(payload)
      if (res.data.code === 200) {
        ElMessage.success('小说发布成功！')
        // 跳转到书城首页
        router.push('/')
      } else {
        ElMessage.error(res.data.msg || '发布失败')
      }
    } catch (error) {
      console.error('发布小说失败:', error)
      ElMessage.error('发布失败，请检查网络或权限')
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.coverUrl = ''
}
</script>

<style scoped>
.write-wrapper {
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.brand {
  font-size: 20px;
  font-weight: 700;
  color: #333;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 30px 20px;
}
.form-card {
  border-radius: 16px;
}
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  width: 200px;
  height: 260px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #fafafa;
  transition: border-color 0.3s;
}
.avatar-uploader:hover {
  border-color: #7c3aed;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>