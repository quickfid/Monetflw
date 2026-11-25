/** Family Budget API. Google Apps Script bound to the family spreadsheet. */
// Оставьте пустым, если скрипт создан из самой таблицы (Расширения → Apps Script).
// Если скрипт отдельный — вставьте ID таблицы из её адреса.
const SPREADSHEET_ID = '';
const OPERATIONS_SHEET = 'Операции';
const REQUESTS_SHEET = '_API_Requests';
const API_TOKEN = 'REPLACE_WITH_A_LONG_RANDOM_SECRET_AT_LEAST_32_CHARS';

function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    const data = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    if (!constantTimeEqual_(String(data.token || ''), API_TOKEN)) return json_({ ok:false, error:'unauthorized' });
    if (data.action !== 'addOperation') return json_({ ok:false, error:'unknown_action' });

    const requestId = String(data.requestId || '').trim();
    if (!/^[A-Za-z0-9-]{16,64}$/.test(requestId)) return json_({ ok:false, error:'invalid_request_id' });
    const amount = Number(String(data.amount).replace(',', '.'));
    const type = String(data.type || '');
    if (!Number.isFinite(amount) || amount <= 0 || amount > 1000000000 || !['Доход','Расход','Перевод'].includes(type)) {
      return json_({ ok:false, error:'invalid_operation' });
    }
    const category = clean_(data.category, 100);
    const comment = clean_(data.comment, 500);
    const person = clean_(data.person, 100);
    const source = clean_(data.sourceAccount, 100);
    const destination = clean_(data.destinationAccount, 100);
    if (!category || !person) return json_({ ok:false, error:'category_and_person_required' });
    if (type === 'Перевод' && (!source || !destination || source === destination)) {
      return json_({ ok:false, error:'transfer_accounts_invalid' });
    }
    if (type !== 'Перевод' && (source || destination)) return json_({ ok:false, error:'accounts_only_for_transfer' });

    lock.waitLock(20000);
    try {
      const ss = openSpreadsheet_();
      const operations = ss.getSheetByName(OPERATIONS_SHEET);
      if (!operations) throw new Error('Не найден лист «Операции»');
      const requests = getRequestsSheet_(ss);
      const existing = findRequest_(requests, requestId);
      if (existing) return json_({ ok:true, duplicate:true, row:existing, message:'already_added' });

      const when = /^\d{4}-\d{2}-\d{2}$/.test(String(data.date || ''))
        ? new Date(String(data.date) + 'T12:00:00') : new Date();
      const row = operations.getLastRow() + 1;
      // A:I: date, amount, type, category, comment, payment method, person, source account, destination account.
      operations.getRange(row, 1, 1, 9).setValues([[
        when, amount, type, category, comment, clean_(data.paymentMethod, 100), person,
        type === 'Перевод' ? source : '', type === 'Перевод' ? destination : ''
      ]]);
      requests.appendRow([requestId, row, new Date()]);
      return json_({ ok:true, duplicate:false, row:row, message:'operation_added' });
    } finally {
      lock.releaseLock();
    }
  } catch (err) {
    return json_({ ok:false, error:'server_error', message:String(err && err.message || err) });
  }
}

function doGet(e) {
  const token = String((e && e.parameter && e.parameter.token) || '');
  if (!constantTimeEqual_(token, API_TOKEN)) return json_({ ok:false, error:'unauthorized' });
  try {
    const ss = openSpreadsheet_();
    const sheet = ss.getSheetByName(OPERATIONS_SHEET);
    if (!sheet) throw new Error('Не найден лист «Операции»');
    return json_({ ok:true, service:'family-budget-api', version:'0.3.0', operationsSheet:true });
  } catch (err) {
    return json_({ ok:false, error:'spreadsheet_access_error', message:String(err.message || err) });
  }
}

function openSpreadsheet_() {
  return SPREADSHEET_ID ? SpreadsheetApp.openById(SPREADSHEET_ID) : SpreadsheetApp.getActiveSpreadsheet();
}

function getRequestsSheet_(ss) {
  let sheet = ss.getSheetByName(REQUESTS_SHEET);
  if (!sheet) {
    sheet = ss.insertSheet(REQUESTS_SHEET);
    sheet.getRange(1, 1, 1, 3).setValues([['request_id','operation_row','created_at']]);
    sheet.hideSheet();
  }
  return sheet;
}
function findRequest_(sheet, requestId) {
  if (sheet.getLastRow() < 2) return 0;
  const cell = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1)
    .createTextFinder(requestId).matchEntireCell(true).findNext();
  return cell ? Number(sheet.getRange(cell.getRow(), 2).getValue()) : 0;
}
function clean_(value, max) { return String(value == null ? '' : value).trim().slice(0, max); }
function constantTimeEqual_(a, b) {
  if (!b || b.startsWith('REPLACE_') || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
