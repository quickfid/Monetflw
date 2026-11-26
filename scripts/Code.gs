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
    if (data.action === 'exportReport') return exportReport_(data);
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
    return json_({ ok:true, service:'family-budget-api', version:'0.5.0', operationsSheet:true });
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

// =====================================================================
//  КРАСИВЫЙ ОТЧЁТ: листы «📊 ГГГГ-ММ», «🧾 ГГГГ-ММ», «📅 ГГГГ» с таблицами и графиками
// =====================================================================
const C = {
  teal: '#1B7F79', tealLight: '#E3F2EF', beige: '#F3EBDD', border: '#C9B98A',
  green: '#E4EBDC', red: '#EFA69C', yellow: '#FDE9C5', paper: '#FBF9F5',
  ink: '#1F2933', gray: '#6B7280', white: '#FFFFFF'
};

function exportReport_(data) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const ss = openSpreadsheet_();
    const meta = data.meta || {};
    const ym = clean_(meta.ym, 7);
    const cur = clean_(meta.currency, 6).replace(/"/g, '') || '₽';
    const fmt = '#,##0 "' + cur + '"';

    const monthSheet = rebuildSheet_(ss, '📊 ' + ym);
    buildMonthReport_(monthSheet, data, fmt);
    const opsSheet = rebuildSheet_(ss, '🧾 ' + ym);
    buildOperations_(opsSheet, data, fmt);
    const year = data.year || {};
    const yearSheet = rebuildSheet_(ss, '📅 ' + String(year.year || ym.slice(0, 4)));
    buildYear_(yearSheet, data, fmt);

    SpreadsheetApp.flush();
    monthSheet.activate();
    return json_({ ok: true, url: ss.getUrl() + '#gid=' + monthSheet.getSheetId(), message: 'report_ready' });
  } catch (err) {
    return json_({ ok: false, error: 'server_error', message: String(err && err.message || err) });
  } finally {
    lock.releaseLock();
  }
}

function rebuildSheet_(ss, name) {
  const old = ss.getSheetByName(name);
  if (old) ss.deleteSheet(old);
  const sheet = ss.insertSheet(name);
  sheet.setHiddenGridlines(true);
  return sheet;
}

function box_(range) {
  range.setBorder(true, true, true, true, true, true, C.border, SpreadsheetApp.BorderStyle.SOLID);
  return range;
}

function titleRow_(sheet, lastCol, text, sub) {
  const t = sheet.getRange(1, 1, 1, lastCol).merge();
  t.setValue(text).setFontSize(20).setFontWeight('bold').setFontColor(C.white)
    .setBackground(C.teal).setVerticalAlignment('middle').setHorizontalAlignment('left');
  sheet.setRowHeight(1, 50);
  const s = sheet.getRange(2, 1, 1, lastCol).merge();
  s.setValue(sub).setFontColor(C.gray).setFontStyle('italic').setBackground(C.paper);
}

function sectionHeader_(sheet, row, lastCol, text) {
  const r = sheet.getRange(row, 1, 1, lastCol).merge();
  r.setValue(text).setFontSize(13).setFontWeight('bold').setFontColor(C.teal)
    .setBorder(false, false, true, false, false, false, C.teal, SpreadsheetApp.BorderStyle.SOLID_MEDIUM);
  sheet.setRowHeight(row, 28);
}

function headerCells_(range) {
  range.setBackground(C.teal).setFontColor(C.white).setFontWeight('bold')
    .setHorizontalAlignment('center').setVerticalAlignment('middle');
  return range;
}

function statusFor_(plan, fact) {
  if (!(plan > 0)) return fact > 0 ? { t: '➕ Вне плана', bg: C.yellow, c: '#B26A00' } : { t: '—', bg: C.white, c: C.gray };
  const ratio = fact / plan;
  if (ratio > 1) return { t: '🔥 Перерасход', bg: C.red, c: '#7A1F17' };
  if (ratio >= 0.85) return { t: '⚠️ Почти лимит', bg: C.yellow, c: '#B26A00' };
  return { t: '✅ В плане', bg: C.green, c: '#1E5B2C' };
}

function buildMonthReport_(sh, data, fmt) {
  const meta = data.meta || {};
  const plan = data.plan || [];
  const daily = data.daily || [];
  const widths = [230, 115, 115, 115, 170, 135, 24];
  widths.forEach(function (w, i) { sh.setColumnWidth(i + 1, w); });

  titleRow_(sh, 6, '💰 Семейный бюджет — ' + clean_(meta.title, 40),
    'Подготовил(а): ' + (clean_(meta.name, 60) || '—') + '  ·  Сформировано: ' + clean_(meta.generated, 10));

  // --- плитки показателей
  const income = Number(meta.income) || 0, expense = Number(meta.expense) || 0;
  const closing = Number(meta.closing) || 0, budget = Number(meta.budget) || 0;
  const projected = Number(meta.projected) || 0;
  const saved = income - expense;
  const labels = [['Доходы', 'Расходы', 'Остаток на конец', 'Накоплено', 'Прогноз расходов', 'План на месяц']];
  const values = [[income, expense, closing, saved, projected, budget]];
  sh.getRange(4, 1, 1, 6).setValues(labels).setBackground(C.beige).setFontWeight('bold')
    .setFontSize(10).setHorizontalAlignment('center');
  const vr = sh.getRange(5, 1, 1, 6);
  vr.setValues(values).setNumberFormat(fmt).setFontWeight('bold').setFontSize(14)
    .setHorizontalAlignment('center').setVerticalAlignment('middle').setBackground(C.white);
  sh.getRange(5, 1).setBackground(C.green);
  sh.getRange(5, 2).setBackground(C.red);
  sh.getRange(5, 5).setBackground(C.yellow);
  sh.getRange(5, 4).setBackground(saved >= 0 ? C.green : C.red);
  sh.setRowHeight(5, 44);
  box_(sh.getRange(4, 1, 2, 6));

  // --- таблица «план и факт»
  sectionHeader_(sh, 7, 6, '🎯 План и факт по категориям');
  const head = sh.getRange(8, 1, 1, 6);
  head.setValues([['Категория', 'План', 'Факт', 'Осталось', 'Выполнение', 'Статус']]);
  headerCells_(head);
  sh.setRowHeight(8, 30);

  const n = plan.length;
  const firstRow = 9;
  if (n > 0) {
    const rows = [], bgs = [], statusCells = [];
    for (let i = 0; i < n; i++) {
      const p = plan[i] || {};
      const pl = Number(p.plan) || 0, fa = Number(p.fact) || 0;
      const r = firstRow + i;
      rows.push([clean_(p.label, 80), pl, fa, '=B' + r + '-C' + r, '', '']);
      const bg = i % 2 === 0 ? C.white : C.paper;
      bgs.push([bg, bg, bg, bg, bg, bg]);
      statusCells.push(statusFor_(pl, fa));
    }
    const body = sh.getRange(firstRow, 1, n, 6);
    body.setValues(rows).setBackgrounds(bgs).setVerticalAlignment('middle');
    sh.getRange(firstRow, 2, n, 3).setNumberFormat(fmt);
    for (let i = 0; i < n; i++) {
      const p = plan[i] || {};
      const pl = Number(p.plan) || 0, fa = Number(p.fact) || 0;
      const r = firstRow + i;
      const ratio = pl > 0 ? Math.min(fa / pl, 1.5) : (fa > 0 ? 1 : 0);
      const color = !(pl > 0) ? '#B26A00' : (fa > pl ? '#C62828' : (fa / pl >= 0.85 ? '#E0A100' : C.teal));
      sh.getRange(r, 5).setFormula('=SPARKLINE(' + ratio + ',{"charttype","bar";"max",1.5;"color1","' + color + '"})');
      const st = statusCells[i];
      sh.getRange(r, 6).setValue(st.t).setBackground(st.bg).setFontColor(st.c).setFontWeight('bold').setHorizontalAlignment('center');
      sh.setRowHeight(r, 26);
    }
    const last = firstRow + n - 1;
    const totalRow = last + 1;
    sh.getRange(totalRow, 1, 1, 6).setValues([[
      'ИТОГО', '=SUM(B' + firstRow + ':B' + last + ')', '=SUM(C' + firstRow + ':C' + last + ')',
      '=B' + totalRow + '-C' + totalRow, '', ''
    ]]).setFontWeight('bold').setBackground(C.beige);
    sh.getRange(totalRow, 2, 1, 3).setNumberFormat(fmt);
    box_(sh.getRange(8, 1, n + 2, 6));
  } else {
    sh.getRange(firstRow, 1, 1, 6).merge().setValue('В этом месяце пока нет данных').setFontColor(C.gray);
  }

  // --- вклады и накопления
  const deps = data.deposits || [];
  const depCount = deps.length;
  const depTop = firstRow + Math.max(n, 1) + 4;
  let dayStart = depTop;
  if (depCount > 0) {
    sectionHeader_(sh, depTop - 1, 6, '🐷 Вклады и накопления');
    const dhead = sh.getRange(depTop, 1, 1, 6);
    dhead.setValues([['Название', 'Сумма', 'Ставка', 'Проценты в месяц', 'Пришло всего', 'Тип']]);
    headerCells_(dhead);
    const deprows = deps.map(function (x) {
      return [clean_(x.label, 80), Number(x.balance) || 0, clean_(x.rate, 30),
        Number(x.monthly) || 0, Number(x.received) || 0, clean_(x.kind, 30)];
    });
    sh.getRange(depTop + 1, 1, depCount, 6).setValues(deprows).setVerticalAlignment('middle');
    sh.getRange(depTop + 1, 2, depCount, 1).setNumberFormat(fmt);
    sh.getRange(depTop + 1, 4, depCount, 2).setNumberFormat(fmt);
    sh.getRange(depTop + 1, 3, depCount, 1).setHorizontalAlignment('center');
    sh.getRange(depTop + 1, 4, depCount, 2).setFontColor('#1E5B2C').setFontWeight('bold');
    box_(sh.getRange(depTop, 1, depCount + 1, 6));
    dayStart = depTop + depCount + 4;
  }

  // --- данные по дням
  sectionHeader_(sh, dayStart - 1, 6, '📅 Расходы по дням');
  const dh = sh.getRange(dayStart, 1, 1, 4);
  dh.setValues([['День', 'Накопительно', 'План на месяц', 'Расход за день']]);
  headerCells_(dh);
  const elapsed = Math.min(Number(meta.elapsed) || 0, daily.length);
  const drows = [];
  let run = 0;
  for (let d = 0; d < daily.length; d++) {
    run += Number(daily[d]) || 0;
    drows.push([d + 1, d < elapsed ? run : '', budget > 0 ? budget : '', Number(daily[d]) || 0]);
  }
  if (drows.length > 0) {
    sh.getRange(dayStart + 1, 1, drows.length, 4).setValues(drows).setHorizontalAlignment('center');
    sh.getRange(dayStart + 1, 2, drows.length, 3).setNumberFormat(fmt);
    box_(sh.getRange(dayStart, 1, drows.length + 1, 4));
  }

  // --- графики справа
  if (n > 0) {
    const last2 = firstRow + n - 1;
    const pie = sh.newChart().setChartType(Charts.ChartType.PIE)
      .addRange(sh.getRange(8, 1, n + 1, 1)).addRange(sh.getRange(8, 3, n + 1, 1))
      .setNumHeaders(1).setPosition(4, 8, 0, 0)
      .setOption('title', 'Куда уходят деньги').setOption('pieHole', 0.45)
      .setOption('width', 560).setOption('height', 320).build();
    sh.insertChart(pie);
    const cols = sh.newChart().setChartType(Charts.ChartType.COLUMN)
      .addRange(sh.getRange(8, 1, n + 1, 3)).setNumHeaders(1).setPosition(22, 8, 0, 0)
      .setOption('title', 'План и факт по категориям').setOption('colors', ['#9CC9C4', '#1B7F79'])
      .setOption('width', 560).setOption('height', 320).build();
    sh.insertChart(cols);
  }
  if (drows.length > 0) {
    const line = sh.newChart().setChartType(Charts.ChartType.LINE)
      .addRange(sh.getRange(dayStart, 1, drows.length + 1, 3)).setNumHeaders(1).setPosition(40, 8, 0, 0)
      .setOption('title', 'Траты за месяц нарастающим итогом и план').setOption('colors', ['#1B7F79', '#C62828'])
      .setOption('width', 560).setOption('height', 320).build();
    sh.insertChart(line);
    const dcol = sh.newChart().setChartType(Charts.ChartType.COLUMN)
      .addRange(sh.getRange(dayStart, 1, drows.length + 1, 1)).addRange(sh.getRange(dayStart, 4, drows.length + 1, 1))
      .setNumHeaders(1).setPosition(58, 8, 0, 0)
      .setOption('title', 'Расходы по дням').setOption('colors', ['#E0A100']).setOption('legend', { position: 'none' })
      .setOption('width', 560).setOption('height', 300).build();
    sh.insertChart(dcol);
  }
  sh.setFrozenRows(2);
}

function buildOperations_(sh, data, fmt) {
  const ops = data.operations || [];
  sh.setColumnWidths(1, 1, 100);
  sh.setColumnWidth(2, 90);
  sh.setColumnWidth(3, 210);
  sh.setColumnWidth(4, 260);
  sh.setColumnWidth(5, 110);
  sh.setColumnWidth(6, 150);
  sh.setColumnWidth(7, 120);
  const head = sh.getRange(1, 1, 1, 7);
  head.setValues([['Дата', 'Тип', 'Категория', 'Комментарий', 'Кто', 'Счёт', 'Сумма']]);
  headerCells_(head);
  sh.setRowHeight(1, 30);
  sh.setFrozenRows(1);
  const n = ops.length;
  if (n === 0) return;
  const rows = [], bgs = [], fonts = [];
  for (let i = 0; i < n; i++) {
    const o = ops[i] || {};
    const d = /^\d{4}-\d{2}-\d{2}$/.test(String(o.date)) ? new Date(String(o.date) + 'T12:00:00') : '';
    rows.push([d, clean_(o.type, 20), clean_(o.category, 80), clean_(o.comment, 200),
      clean_(o.person, 60), clean_(o.account, 80), Number(o.amount) || 0]);
    const bg = i % 2 === 0 ? C.white : C.paper;
    bgs.push([bg, bg, bg, bg, bg, bg, bg]);
    const col = o.type === 'Доход' ? '#1E5B2C' : (o.type === 'Расход' ? '#C62828' : C.ink);
    fonts.push([C.ink, C.ink, C.ink, C.ink, C.ink, C.ink, col]);
  }
  const body = sh.getRange(2, 1, n, 7);
  body.setValues(rows).setBackgrounds(bgs).setFontColors(fonts).setVerticalAlignment('middle');
  sh.getRange(2, 1, n, 1).setNumberFormat('dd.MM.yyyy').setHorizontalAlignment('center');
  sh.getRange(2, 7, n, 1).setNumberFormat(fmt).setFontWeight('bold');
  box_(sh.getRange(1, 1, n + 1, 7));
  sh.getRange(1, 1, n + 1, 7).createFilter();
}

function buildYear_(sh, data, fmt) {
  const meta = data.meta || {};
  const year = data.year || {};
  const months = year.months || [];
  const top = year.top || [];
  [130, 120, 120, 120, 150, 24].forEach(function (w, i) { sh.setColumnWidth(i + 1, w); });
  titleRow_(sh, 5, '📅 Итоги года — ' + String(year.year || ''),
    'Подготовил(а): ' + (clean_(meta.name, 60) || '—') + '  ·  Сформировано: ' + clean_(meta.generated, 10));

  sectionHeader_(sh, 4, 5, '📈 По месяцам');
  const head = sh.getRange(5, 1, 1, 5);
  head.setValues([['Месяц', 'Доходы', 'Расходы', 'Разница', 'Накопления']]);
  headerCells_(head);
  const rows = [], bgs = [];
  for (let i = 0; i < 12; i++) {
    const m = months[i] || {};
    const r = 6 + i;
    rows.push([clean_(m.name, 10) || String(i + 1), Number(m.income) || 0, Number(m.expense) || 0,
      '=B' + r + '-C' + r, '=SUM(D$6:D' + r + ')']);
    const bg = i % 2 === 0 ? C.white : C.paper;
    bgs.push([bg, bg, bg, bg, bg]);
  }
  sh.getRange(6, 1, 12, 5).setValues(rows).setBackgrounds(bgs);
  sh.getRange(6, 2, 12, 4).setNumberFormat(fmt);
  sh.getRange(18, 1, 1, 5).setValues([['ИТОГО', '=SUM(B6:B17)', '=SUM(C6:C17)', '=SUM(D6:D17)', '']])
    .setFontWeight('bold').setBackground(C.beige);
  sh.getRange(18, 2, 1, 3).setNumberFormat(fmt);
  box_(sh.getRange(5, 1, 14, 5));

  sectionHeader_(sh, 21, 5, '🏆 ТОП категорий расходов за год');
  const th = sh.getRange(22, 1, 1, 2);
  th.setValues([['Категория', 'Сумма']]);
  headerCells_(th);
  const t = top.length;
  if (t > 0) {
    const trows = top.map(function (x) { return [clean_(x.label, 80), Number(x.amount) || 0]; });
    sh.getRange(23, 1, t, 2).setValues(trows);
    sh.getRange(23, 2, t, 1).setNumberFormat(fmt);
    box_(sh.getRange(22, 1, t + 1, 2));
  }
  sh.setColumnWidth(1, 230);

  const bars = sh.newChart().setChartType(Charts.ChartType.COLUMN)
    .addRange(sh.getRange(5, 1, 13, 3)).setNumHeaders(1).setPosition(4, 7, 0, 0)
    .setOption('title', 'Доходы и расходы по месяцам').setOption('colors', ['#2E9E5B', '#C62828'])
    .setOption('width', 600).setOption('height', 320).build();
  sh.insertChart(bars);
  const saving = sh.newChart().setChartType(Charts.ChartType.LINE)
    .addRange(sh.getRange(5, 1, 13, 1)).addRange(sh.getRange(5, 5, 13, 1)).setNumHeaders(1).setPosition(22, 7, 0, 0)
    .setOption('title', 'Накопления в течение года').setOption('colors', ['#1B7F79'])
    .setOption('width', 600).setOption('height', 300).build();
  sh.insertChart(saving);
  if (t > 0) {
    const topChart = sh.newChart().setChartType(Charts.ChartType.BAR)
      .addRange(sh.getRange(22, 1, t + 1, 2)).setNumHeaders(1).setPosition(39, 7, 0, 0)
      .setOption('title', 'ТОП категорий года').setOption('colors', ['#E0A100']).setOption('legend', { position: 'none' })
      .setOption('width', 600).setOption('height', 380).build();
    sh.insertChart(topChart);
  }
}
