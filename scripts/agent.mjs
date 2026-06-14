import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const userMessage   = process.env.ISSUE_BODY || '';
const issueNumber   = process.env.ISSUE_NUMBER;
const repo          = process.env.REPO;
const token         = process.env.GH_TOKEN;

// ── قراءة هيكل الملفات ──
function getFileTree(dir = '.', prefix = '') {
  let result = '';
  try {
    const ignored = ['.git', 'node_modules', 'scripts', '.github'];
    const items = fs.readdirSync(dir).filter(f => !ignored.includes(f));
    for (const item of items) {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        result += `${prefix}📁 ${item}/\n`;
        result += getFileTree(fullPath, prefix + '  ');
      } else {
        result += `${prefix}📄 ${item}\n`;
      }
    }
  } catch(e) {}
  return result;
}

const fileTree = getFileTree('.');
console.log('📨 طلب المستخدم:', userMessage);

// ── استدعاء Gemini ──
const geminiRes = await fetch(
  `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`,
  {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      system_instruction: {
        parts: [{
          text: `أنت مساعد ذكاء اصطناعي متخصص في البرمجة تعمل داخل GitHub repository.

هيكل الملفات الحالية:
${fileTree}

يمكنك تنفيذ العمليات التالية بهذه الصيغ بالضبط:

===CREATE_FILE===
PATH: المسار/للملف
CONTENT:
محتوى الملف
===END_FILE===

===EDIT_FILE===
PATH: المسار/للملف
CONTENT:
المحتوى الجديد الكامل
===END_FILE===

===DELETE_FILE===
PATH: المسار/للملف
===END_DELETE===

===RUN_COMMAND===
COMMAND: الأمر
===END_COMMAND===

أجب دائماً بالعربية واشرح ما ستفعله قبل تنفيذه.`
        }]
      },
      contents: [{
        role: 'user',
        parts: [{ text: userMessage }]
      }],
      generationConfig: {
        maxOutputTokens: 4096,
        temperature: 0.7
      }
    })
  }
);

const geminiData = await geminiRes.json();
const aiResponse = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || '';

if (!aiResponse) {
  console.error('❌ لم يرد Gemini:', JSON.stringify(geminiData));
  process.exit(1);
}

console.log('🤖 رد Gemini:', aiResponse);

let executionLog = '';

// ── تنفيذ CREATE_FILE ──
for (const match of aiResponse.matchAll(/===CREATE_FILE===\s*PATH:\s*(.+?)\s*CONTENT:\s*([\s\S]*?)===END_FILE===/g)) {
  const filePath = match[1].trim();
  const content  = match[2].trim();
  try {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, content);
    executionLog += `✅ تم إنشاء: ${filePath}\n`;
  } catch(e) {
    executionLog += `❌ فشل إنشاء: ${filePath} — ${e.message}\n`;
  }
}

// ── تنفيذ EDIT_FILE ──
for (const match of aiResponse.matchAll(/===EDIT_FILE===\s*PATH:\s*(.+?)\s*CONTENT:\s*([\s\S]*?)===END_FILE===/g)) {
  const filePath = match[1].trim();
  const content  = match[2].trim();
  try {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, content);
    executionLog += `✅ تم تعديل: ${filePath}\n`;
  } catch(e) {
    executionLog += `❌ فشل تعديل: ${filePath} — ${e.message}\n`;
  }
}

// ── تنفيذ DELETE_FILE ──
for (const match of aiResponse.matchAll(/===DELETE_FILE===\s*PATH:\s*(.+?)\s*===END_DELETE===/g)) {
  const filePath = match[1].trim();
  try {
    fs.unlinkSync(filePath);
    executionLog += `✅ تم حذف: ${filePath}\n`;
  } catch(e) {
    executionLog += `❌ فشل حذف: ${filePath} — ${e.message}\n`;
  }
}

// ── تنفيذ RUN_COMMAND ──
for (const match of aiResponse.matchAll(/===RUN_COMMAND===\s*COMMAND:\s*(.+?)\s*===END_COMMAND===/g)) {
  const cmd = match[1].trim();
  try {
    const output = execSync(cmd, { encoding: 'utf8', timeout: 30000 });
    executionLog += `✅ تم تشغيل: ${cmd}\n${output}\n`;
  } catch(e) {
    executionLog += `⚠️ أمر: ${cmd}\n${e.message}\n`;
  }
}

// ── Git commit إذا تغير شيء ──
try {
  const status = execSync('git status --porcelain', { encoding: 'utf8' });
  if (status.trim()) {
    execSync('git config user.name "Younes AI Agent"');
    execSync('git config user.email "ai@younes.dev"');
    execSync('git add -A');
    execSync(`git commit -m "🤖 ${userMessage.substring(0, 60)}"`);
    execSync('git push origin HEAD');
    executionLog += `\n✅ تم حفظ التغييرات في المستودع\n`;
  }
} catch(e) {
  console.log('Git:', e.message);
}

// ── تنظيف الرد ──
const cleanResponse = aiResponse
  .replace(/===CREATE_FILE===[\s\S]*?===END_FILE===/g, '')
  .replace(/===EDIT_FILE===[\s\S]*?===END_FILE===/g, '')
  .replace(/===DELETE_FILE===[\s\S]*?===END_DELETE===/g, '')
  .replace(/===RUN_COMMAND===[\s\S]*?===END_COMMAND===/g, '')
  .trim();

// ── نشر الرد على GitHub Issue ──
const replyBody = `## 🤖 رد المساعد

${cleanResponse}

${executionLog ? `---\n### 📋 العمليات المنفذة:\n\`\`\`\n${executionLog}\`\`\`` : ''}

---
*⏱️ Younes AI Agent — kassousyounes70-bit/YOUNES-AI*`;

await fetch(`https://api.github.com/repos/${repo}/issues/${issueNumber}/comments`, {
  method: 'POST',
  headers: {
    'Authorization': `token ${token}`,
    'Content-Type': 'application/json',
    'Accept': 'application/vnd.github.v3+json'
  },
  body: JSON.stringify({ body: replyBody })
});

// ── إغلاق الـ Issue ──
await fetch(`https://api.github.com/repos/${repo}/issues/${issueNumber}`, {
  method: 'PATCH',
  headers: {
    'Authorization': `token ${token}`,
    'Content-Type': 'application/json',
    'Accept': 'application/vnd.github.v3+json'
  },
  body: JSON.stringify({ state: 'closed', labels: ['ai-done'] })
});

console.log('✅ تم إرسال الرد وإغلاق الـ Issue');
