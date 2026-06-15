import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const GH_TOKEN    = process.env.GH_TOKEN;
const userMessage = process.env.ISSUE_BODY || '';
const issueNumber = process.env.ISSUE_NUMBER;
const repo        = process.env.REPO;
const issueTitle  = process.env.ISSUE_TITLE || '';

// ── استخراج النموذج من عنوان الـ Issue ──
function getModel(title) {
  if (title.includes('[deepseek]'))  return 'DeepSeek-V3-0324';
  if (title.includes('[gpt5]'))      return 'gpt-4o';
  if (title.includes('[compare]'))   return 'compare';
  return 'meta-llama/Llama-4-Scout-17B-16E-Instruct';
}

const selectedModel = getModel(issueTitle.toLowerCase());

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

// ── قراءة الذاكرة ──
function loadMemory() {
  try {
    if (fs.existsSync('memory.json')) {
      const data = JSON.parse(fs.readFileSync('memory.json', 'utf8'));
      return data.slice(-10);
    }
  } catch(e) {}
  return [];
}

// ── حفظ الذاكرة ──
function saveMemory(history) {
  try {
    const limited = history.slice(-20);
    fs.writeFileSync('memory.json', JSON.stringify(limited, null, 2));
  } catch(e) {}
}

const fileTree = getFileTree('.');
const memory   = loadMemory();

console.log('📨 طلب المستخدم:', userMessage);
console.log('🤖 النموذج:', selectedModel);

// ── System Prompt عام لأي موضوع ──
const systemPrompt = `أنت مساعد ذكاء اصطناعي متعدد المهام وذكي وودود. تستطيع المساعدة في أي موضوع يطلبه المستخدم سواء كان:
- أسئلة عامة ومعلومات
- برمجة وتقنية
- تفسير أحلام
- نصائح وإرشادات
- ترجمة وتحرير نصوص
- رياضيات وعلوم
- أدب وشعر وقصص
- أي موضوع آخر

عند طلب مساعدة برمجية داخل GitHub repository يمكنك تنفيذ:

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

هيكل الملفات الحالية في المشروع:
${fileTree}

تحدث دائماً بالعربية بأسلوب ودي ومفيد.`;

// ── بناء رسائل المحادثة مع الذاكرة ──
const messages = [
  ...memory,
  { role: 'user', content: userMessage }
];

// ── استدعاء نموذج واحد ──
async function callModel(modelId) {
  try {
    const res = await fetch('https://models.inference.ai.azure.com/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${GH_TOKEN}`
      },
      body: JSON.stringify({
        model: modelId,
        max_tokens: 4096,
        messages: [
          { role: 'system', content: systemPrompt },
          ...messages
        ]
      })
    });
    const data = await res.json();
    if (data.error) {
      console.error(`❌ خطأ في ${modelId}:`, data.error.message);
      return `⚠️ النموذج ${modelId} غير متاح حالياً`;
    }
    return data.choices?.[0]?.message?.content || '⚠️ لا يوجد رد';
  } catch(e) {
    console.error(`❌ استثناء في ${modelId}:`, e.message);
    return `⚠️ فشل الاتصال بـ ${modelId}`;
  }
}

// ── تنفيذ العمليات ──
function executeOperations(aiResponse) {
  let executionLog = '';

  for (const match of aiResponse.matchAll(/===CREATE_FILE===\s*PATH:\s*(.+?)\s*CONTENT:\s*([\s\S]*?)===END_FILE===/g)) {
    const filePath = match[1].trim();
    const content  = match[2].trim();
    try {
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, content);
      executionLog += `✅ تم إنشاء: ${filePath}\n`;
    } catch(e) {
      executionLog += `❌ فشل: ${filePath} — ${e.message}\n`;
    }
  }

  for (const match of aiResponse.matchAll(/===EDIT_FILE===\s*PATH:\s*(.+?)\s*CONTENT:\s*([\s\S]*?)===END_FILE===/g)) {
    const filePath = match[1].trim();
    const content  = match[2].trim();
    try {
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, content);
      executionLog += `✅ تم تعديل: ${filePath}\n`;
    } catch(e) {
      executionLog += `❌ فشل: ${filePath} — ${e.message}\n`;
    }
  }

  for (const match of aiResponse.matchAll(/===DELETE_FILE===\s*PATH:\s*(.+?)\s*===END_DELETE===/g)) {
    const filePath = match[1].trim();
    try {
      fs.unlinkSync(filePath);
      executionLog += `✅ تم حذف: ${filePath}\n`;
    } catch(e) {
      executionLog += `❌ فشل: ${filePath} — ${e.message}\n`;
    }
  }

  for (const match of aiResponse.matchAll(/===RUN_COMMAND===\s*COMMAND:\s*(.+?)\s*===END_COMMAND===/g)) {
    const cmd = match[1].trim();
    try {
      const output = execSync(cmd, { encoding: 'utf8', timeout: 30000 });
      executionLog += `✅ تم: ${cmd}\n${output}\n`;
    } catch(e) {
      executionLog += `⚠️ ${cmd}\n${e.message}\n`;
    }
  }

  return executionLog;
}

// ── تنظيف الرد ──
function cleanResponse(text) {
  return text
    .replace(/===CREATE_FILE===[\s\S]*?===END_FILE===/g, '')
    .replace(/===EDIT_FILE===[\s\S]*?===END_FILE===/g, '')
    .replace(/===DELETE_FILE===[\s\S]*?===END_DELETE===/g, '')
    .replace(/===RUN_COMMAND===[\s\S]*?===END_COMMAND===/g, '')
    .trim();
}

let replyBody    = '';
let executionLog = '';

// ── وضع المقارنة ──
if (selectedModel === 'compare') {
  console.log('🔀 وضع المقارنة — 3 نماذج');

  const [r1, r2, r3] = await Promise.all([
    callModel('meta-llama/Llama-4-Scout-17B-16E-Instruct'),
    callModel('DeepSeek-V3-0324'),
    callModel('gpt-4o')
  ]);

  executionLog = executeOperations(r1);

  // ── تنسيق خاص للمقارنة يفهمه التطبيق ──
  replyBody = `##COMPARE##
##MODEL1##
${cleanResponse(r1)}
##MODEL2##
${cleanResponse(r2)}
##MODEL3##
${cleanResponse(r3)}
##END##
${executionLog ? `\n📋 العمليات:\n${executionLog}` : ''}

---
*⏱️ Younes AI — وضع المقارنة*`;

} else {
  // ── نموذج واحد ──
  console.log('🤖 نموذج واحد:', selectedModel);

  const aiResponse = await callModel(selectedModel);
  executionLog     = executeOperations(aiResponse);

  memory.push({ role: 'user', content: userMessage });
  memory.push({ role: 'assistant', content: aiResponse });
  saveMemory(memory);

  // ── Git commit ──
  try {
    const status = execSync('git status --porcelain', { encoding: 'utf8' });
    if (status.trim()) {
      execSync('git config user.name "Younes AI Agent"');
      execSync('git config user.email "ai@younes.dev"');
      execSync('git add -A');
      execSync(`git commit -m "🤖 ${userMessage.substring(0, 60)}"`);
      execSync('git push origin HEAD');
      executionLog += `\n✅ تم حفظ التغييرات\n`;
    }
  } catch(e) {
    console.log('Git:', e.message);
  }

  replyBody = `## 🤖 رد المساعد

${cleanResponse(aiResponse)}

${executionLog ? `---\n### 📋 العمليات:\n\`\`\`\n${executionLog}\`\`\`` : ''}

---
*⏱️ Younes AI Agent — kassousyounes70-bit/YOUNES-AI*`;
}

// ── نشر الرد ──
await fetch(`https://api.github.com/repos/${repo}/issues/${issueNumber}/comments`, {
  method: 'POST',
  headers: {
    'Authorization': `token ${GH_TOKEN}`,
    'Content-Type': 'application/json',
    'Accept': 'application/vnd.github.v3+json'
  },
  body: JSON.stringify({ body: replyBody })
});

// ── إغلاق الـ Issue ──
await fetch(`https://api.github.com/repos/${repo}/issues/${issueNumber}`, {
  method: 'PATCH',
  headers: {
    'Authorization': `token ${GH_TOKEN}`,
    'Content-Type': 'application/json',
    'Accept': 'application/vnd.github.v3+json'
  },
  body: JSON.stringify({ state: 'closed', labels: ['ai-done'] })
});

console.log('✅ تم إرسال الرد وإغلاق الـ Issue');
