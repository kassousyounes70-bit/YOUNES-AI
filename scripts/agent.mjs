import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const GH_TOKEN    = process.env.GH_TOKEN;
const userMessage = process.env.ISSUE_BODY || '';
const issueNumber = process.env.ISSUE_NUMBER;
const repo        = process.env.REPO;
const issueTitle  = process.env.ISSUE_TITLE || '';

// ── استخراج النموذج ──
function getModel(title) {
  if (title.includes('[deepseek]')) return 'DeepSeek-V3-0324';
  if (title.includes('[gpt5]'))     return 'gpt-4o';
  if (title.includes('[phi4]'))     return 'Phi-4';
  if (title.includes('[compare]'))  return 'compare';
  return 'Phi-4'; // افتراضي بدل Llama
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

// ── ذاكرة منفصلة لكل نموذج ──
function getMemoryFile(model) {
  const safe = model.replace(/[^a-zA-Z0-9]/g, '_');
  return `memory_${safe}.json`;
}

function loadMemory(model) {
  try {
    const file = getMemoryFile(model);
    if (fs.existsSync(file)) {
      const data = JSON.parse(fs.readFileSync(file, 'utf8'));
      return data.slice(-10);
    }
  } catch(e) {}
  return [];
}

function saveMemory(model, history) {
  try {
    const file = getMemoryFile(model);
    fs.writeFileSync(file, JSON.stringify(history.slice(-20), null, 2));
  } catch(e) {}
}

const fileTree = getFileTree('.');
console.log('📨 طلب المستخدم:', userMessage);
console.log('🤖 النموذج:', selectedModel);

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

هيكل الملفات الحالية:
${fileTree}

تحدث دائماً بالعربية بأسلوب ودي ومفيد.`;

// ── استدعاء نموذج ──
async function callModel(modelId, memory) {
  try {
    const messages = [
      ...memory,
      { role: 'user', content: userMessage }
    ];

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
      return `⚠️ النموذج ${modelId} غير متاح: ${data.error.message}`;
    }
    return data.choices?.[0]?.message?.content || '⚠️ لا يوجد رد';
  } catch(e) {
    return `⚠️ فشل الاتصال: ${e.message}`;
  }
}

// ── تنفيذ العمليات ──
function executeOperations(aiResponse) {
  let log = '';

  for (const match of aiResponse.matchAll(
      /===CREATE_FILE===\s*PATH:\s*(.+?)\s*CONTENT:\s*([\s\S]*?)===END_FILE===/g)) {
    const filePath = match[1].trim();
    const content  = match[2].trim();
    try {
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, content);
      log += `✅ إنشاء: ${filePath}\n`;
    } catch(e) { log += `❌ فشل: ${filePath}\n`; }
  }

  for (const match of aiResponse.matchAll(
      /===EDIT_FILE===\s*PATH:\s*(.+?)\s*CONTENT:\s*([\s\S]*?)===END_FILE===/g)) {
    const filePath = match[1].trim();
    const content  = match[2].trim();
    try {
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, content);
      log += `✅ تعديل: ${filePath}\n`;
    } catch(e) { log += `❌ فشل: ${filePath}\n`; }
  }

  for (const match of aiResponse.matchAll(
      /===DELETE_FILE===\s*PATH:\s*(.+?)\s*===END_DELETE===/g)) {
    try {
      fs.unlinkSync(match[1].trim());
      log += `✅ حذف: ${match[1].trim()}\n`;
    } catch(e) { log += `❌ فشل حذف\n`; }
  }

  for (const match of aiResponse.matchAll(
      /===RUN_COMMAND===\s*COMMAND:\s*(.+?)\s*===END_COMMAND===/g)) {
    try {
      const out = execSync(match[1].trim(), { encoding: 'utf8', timeout: 30000 });
      log += `✅ تشغيل: ${match[1].trim()}\n${out}\n`;
    } catch(e) { log += `⚠️ ${match[1].trim()}\n`; }
  }

  return log;
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

let replyBody = '';

// ── وضع المقارنة ──
if (selectedModel === 'compare') {
  console.log('🔀 وضع المقارنة');

  // كل نموذج يستخدم ذاكرته المستقلة
  const mem1 = loadMemory('Phi-4');
  const mem2 = loadMemory('DeepSeek-V3-0324');
  const mem3 = loadMemory('gpt-4o');

  const [r1, r2, r3] = await Promise.all([
    callModel('Phi-4',            mem1),
    callModel('DeepSeek-V3-0324', mem2),
    callModel('gpt-4o',           mem3)
  ]);

  // حفظ الذاكرة لكل نموذج بشكل منفصل
  mem1.push({ role: 'user', content: userMessage });
  mem1.push({ role: 'assistant', content: r1 });
  saveMemory('Phi-4', mem1);

  mem2.push({ role: 'user', content: userMessage });
  mem2.push({ role: 'assistant', content: r2 });
  saveMemory('DeepSeek-V3-0324', mem2);

  mem3.push({ role: 'user', content: userMessage });
  mem3.push({ role: 'assistant', content: r3 });
  saveMemory('gpt-4o', mem3);

  executeOperations(r1);

  replyBody = `##COMPARE##
##MODEL1##
${cleanResponse(r1)}
##MODEL2##
${cleanResponse(r2)}
##MODEL3##
${cleanResponse(r3)}
##END##

---
*⏱️ Younes AI — وضع المقارنة*`;

} else {
  // ── نموذج واحد بذاكرته المستقلة ──
  const memory     = loadMemory(selectedModel);
  const aiResponse = await callModel(selectedModel, memory);
  const execLog    = executeOperations(aiResponse);

  memory.push({ role: 'user', content: userMessage });
  memory.push({ role: 'assistant', content: aiResponse });
  saveMemory(selectedModel, memory);

  // ── Git commit ──
  try {
    const status = execSync('git status --porcelain', { encoding: 'utf8' });
    if (status.trim()) {
      execSync('git config user.name "Younes AI Agent"');
      execSync('git config user.email "ai@younes.dev"');
      execSync('git add -A');
      execSync(`git commit -m "🤖 ${userMessage.substring(0, 60)}"`);
      execSync('git push origin HEAD');
    }
  } catch(e) { console.log('Git:', e.message); }

  replyBody = `## 🤖 رد المساعد

${cleanResponse(aiResponse)}

${execLog ? `---\n### 📋 العمليات:\n\`\`\`\n${execLog}\`\`\`` : ''}

---
*⏱️ Younes AI — ${selectedModel}*`;
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

console.log('✅ تم الإرسال');
