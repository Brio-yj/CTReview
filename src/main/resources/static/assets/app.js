// ================== API & UTILS ==================
const API = {
    add: () => `/api/problems`,
    today: () => `/api/reviews/today`,
    solveAny: (params) => `/api/problems/solve?${params.toString()}`,
    failAny:  (params) => `/api/problems/fail?${params.toString()}`,
    graduateAny:(params) => `/api/problems/graduate?${params.toString()}`,
    deleteAny:(params) => `/api/problems?${params.toString()}`,
    search:   (params) => `/api/problems?${params.toString()}`,
    dashboard:() => `/api/dashboard/summary`
};

async function http(method, url, body) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body !== undefined && body !== null) opts.body = JSON.stringify(body);
    const res = await fetch(url, opts);
    if (!res.ok) {
        let msg = `${res.status} ${res.statusText}`;
        try { const j = await res.json(); msg = j.message || JSON.stringify(j); } catch {}
        throw new Error(msg);
    }
    if (res.status === 204) return null;
    return res.json();
}
const el = (id) => document.getElementById(id);
function fmtDate(d){ return d ? d.replace(/-/g,'.') : '-'; }
const diffMap = {HIGH:'상', MEDIUM:'중', LOW:'하'};

// ================== CORE LOGIC ==================

// ---- 테마 변경 ----
function applyTheme(theme){
    document.body.classList.toggle('light', theme === 'light');
    const btn = el('btn-theme');
    if (btn) btn.textContent = theme === 'light' ? '다크 모드' : '라이트 모드';
    loadDashboard(); // 테마 변경 시 대시보드(차트, 히트맵) 다시 그리기
}
function toggleTheme(){
    const currentTheme = document.body.classList.contains('light') ? 'light' : 'dark';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    localStorage.setItem('theme', newTheme);
    applyTheme(newTheme);
}

// ---- Toast ----
function toastHost(){
    let h = el('toast-host');
    if(!h){ h = document.createElement('div'); h.id='toast-host';
        Object.assign(h.style,{position:'fixed',top:'16px',right:'16px',display:'flex',flexDirection:'column',gap:'8px',zIndex:'9999'});
        document.body.appendChild(h);
    }
    return h;
}
function toast(msg,type='info'){
    const div = document.createElement('div');
    div.className = `toast ${type}`;
    div.textContent = msg;
    toastHost().appendChild(div);
    setTimeout(()=>div.remove(), 2200);
}

// ---- Data Load & Render ----
/**
 * 테이블 행(TR) 생성 함수
 * @param {object} p - 문제 데이터
 * @param {'today'|'search'} type - 테이블 종류
 */
function createProblemRow(p, type) {
    const tr = document.createElement('tr');
    const tpl = el('row-actions').content.cloneNode(true);
    const [btnSolve, btnFail, btnDelete, btnGraduate] = tpl.querySelectorAll('button');

    btnDelete.addEventListener('click', (e) => { e.stopPropagation(); delBy(p, btnDelete); });

    const diffTxt = diffMap[p.difficulty] || p.difficulty;
    let cells = `
        <td>${p.category ?? '-'}</td>
        <td>${p.number ?? '-'}</td>
        <td>${p.name}</td>
        <td>${diffTxt}</td>
    `;
    const actionTd = document.createElement('td');

    if (type === 'today') {
        cells += `<td>${p.reviewStep}</td>`;
        btnSolve.addEventListener('click', (e) => { e.stopPropagation(); actBy('solve', p, btnSolve, btnFail); });
        btnFail.addEventListener('click', (e) => { e.stopPropagation(); actBy('fail', p, btnSolve, btnFail); });
        btnDelete.style.display = 'none';
        btnGraduate.style.display = 'none';
        actionTd.appendChild(tpl);
    } else { // type === 'search'
        cells += `<td>${fmtDate(p.nextReviewDate)}</td>`;
        cells += `<td>${p.reviewStep}</td>`;
        const graduateCell = document.createElement('td');
        if (p.status !== 'GRADUATED') {
            btnGraduate.addEventListener('click', (e) => { e.stopPropagation(); actBy('graduate', p, btnGraduate); });
            graduateCell.appendChild(btnGraduate);
        } else {
            graduateCell.textContent = '✔️';
        }
        btnSolve.style.display = 'none';
        btnFail.style.display = 'none';
        actionTd.appendChild(btnDelete);
        tr.innerHTML = cells;
        tr.appendChild(graduateCell);
        tr.appendChild(actionTd);
        return tr;
    }

    tr.innerHTML = cells;
    tr.appendChild(actionTd);
    return tr;
}

async function loadToday(){
    const tbody = el('tbl-today');
    try {
        const list = await http('GET', API.today());
        tbody.innerHTML='';
        if (!list || !list.length){
            tbody.innerHTML = `<tr><td colspan="6" style="color:var(--muted)">오늘 복습할 문제가 없습니다.</td></tr>`;
        } else {
            list.forEach(p => tbody.appendChild(createProblemRow(p, 'today')));
        }
    } catch(e){
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--bad)">오늘 목록 로드 실패: ${e.message}</td></tr>`;
    }
}

async function performSearch(){
    const tbody = el('tbl-search');
    try{
        const params = new URLSearchParams();
        const n = el('s-number')?.value; const q = el('s-q')?.value?.trim();
        const diff = el('s-difficulty')?.value;  const from = el('s-from')?.value; const to = el('s-to')?.value;
        const sort = el('s-sort')?.value;
        if (n) params.set('number', n); if (q) params.set('q', q); if (diff) params.set('difficulty', diff);
        if (from) params.set('from', from); if (to) params.set('to', to); if (sort) params.set('sort', sort);

        const list = await http('GET', API.search(params));
        tbody.innerHTML='';
        if (!list || !list.length){
            tbody.innerHTML = `<tr><td colspan="8" style="color:var(--muted)">검색 결과가 없습니다.</td></tr>`;
        } else {
            list.forEach(p => tbody.appendChild(createProblemRow(p, 'search')));
        }
    } catch(e){
        tbody.innerHTML = `<tr><td colspan="8" style="color:var(--bad)">검색 실패: ${e.message}</td></tr>`;
    }
}

// ---- Actions ----
async function addProblem() {
    const nameInput = el('p-name');
    const name = nameInput.value.trim();
    if (!name) return toast('문제 이름을 입력해 주세요', 'bad');
    const numVal = el('p-number').value; const number = numVal ? parseInt(numVal,10) : null;
    const category = el('p-category')?.value || null;
    const difficulty = el('p-difficulty').value;
    const payload = { name, difficulty };
    if (number !== null && !Number.isNaN(number)) payload.number = number;
    if (category) payload.category = category;
    try {
        await http('POST', API.add(), payload);
        el('p-number').value=''; nameInput.value=''; el('p-difficulty').value='MEDIUM';
        toast('문제 추가 완료','ok');
        Promise.all([loadToday(), performSearch(), loadDashboard()]);
    } catch(e){ toast('추가 실패: '+e.message, 'bad'); }
}
async function actBy(kind, problem, ...btns){
    try{
        btns.forEach(b=>b && (b.disabled=true));
        const params = new URLSearchParams();
        if (problem?.name) { params.set('name', problem.name); }
        else if (problem?.number != null) { params.set('number', problem.number); }
        const url = (kind==='solve') ? API.solveAny(params)
            : (kind==='fail') ? API.failAny(params)
            : API.graduateAny(params);
        await http('POST', url);
        toast(`${kind.toUpperCase()} 완료`, 'ok');
        Promise.all([loadToday(), performSearch(), loadDashboard()]);
    } catch(e){ toast(`${kind.toUpperCase()} 실패: `+e.message, 'bad');
    } finally{ btns.forEach(b=>b && (b.disabled=false)); }
}
async function delBy(problem, ...btns){
    if (!confirm(`[문제 삭제]\n${problem.name}\n\n정말로 삭제하시겠습니까?`)) return;
    try{
        btns.forEach(b=>b && (b.disabled=true));
        const params = new URLSearchParams();
        if (problem?.name) { params.set('name', problem.name); }
        else if (problem?.number != null) { params.set('number', problem.number); }
        await http('DELETE', API.deleteAny(params));
        toast('삭제 완료', 'ok');
        Promise.all([loadToday(), performSearch(), loadDashboard()]);
    } catch(e){ toast('삭제 실패: '+e.message, 'bad');
    } finally{ btns.forEach(b=>b && (b.disabled=false)); }
}
async function quickAction(kind){
    const name = el('quick-name').value.trim();
    if (!name) return toast('문제 이름을 입력해 주세요', 'bad');
    await actBy(kind, {name});
}

// ---- 월간 히트맵 ----
let heatmapDate = new Date();
let heatmapData = new Map();

function renderMonthlyHeatmap() {
    const grid = el('heatmap-grid');
    const monthEl = el('heatmap-month');
    const dayHeader = el('heatmap-day-header');
    if(!grid || !monthEl) return;

    grid.innerHTML = '';
    dayHeader.innerHTML = ['일', '월', '화', '수', '목', '금', '토'].map(d => `<div>${d}</div>`).join('');

    heatmapDate.setDate(1);
    const year = heatmapDate.getFullYear();
    const month = heatmapDate.getMonth();
    monthEl.textContent = `${year}년 ${month + 1}월`;

    const firstDay = heatmapDate.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    for (let i = 0; i < firstDay; i++) {
        grid.appendChild(document.createElement('div'));
    }

    const maxVal = Math.max(...heatmapData.values(), 1);
    for (let day = 1; day <= daysInMonth; day++) {
        const cell = document.createElement('div');
        cell.className = 'heatmap-cell';
        //cell.textContent = day;
        const key = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const val = heatmapData.get(key) ?? 0;
        if (val > 0) {
            cell.classList.add('has-data');
            const level = Math.min(4, Math.ceil(val / (maxVal / 4)));
            cell.dataset.level = String(level);
            cell.title = `${key}: ${val} solved`;
        }
        grid.appendChild(cell);
    }
}

async function loadDashboard(){
    try{
        const data = await http('GET', API.dashboard());
        el('streak').textContent=`연속일: ${data.streak}일`;
        el('today-text').textContent=data.today;
        const dist=data.stepDistribution||{}; const box=el('level-dist');
        if(box){ box.innerHTML=''; [1,2,3].forEach(l=>{ const v=dist[l]||0; if(v>0){const row=document.createElement('div');
            row.innerHTML=`단계 ${l} × ${v}`; box.appendChild(row); }});}

        const style = getComputedStyle(document.body);
        const chartColors = {
            grid: style.getPropertyValue('--chart-grid').trim(),
            label: style.getPropertyValue('--chart-label').trim(),
            bar: style.getPropertyValue('--chart-bar').trim()
        };

        const daily=Array.isArray(data.daily)?data.daily:[]; const dL=daily.map(d=>d.date), dV=daily.map(d=>(+d.count||0));
        drawBarChart(el('chart-daily'), dL, dV, chartColors);
        const grads=Array.isArray(data.graduations)?data.graduations:[]; const gL=grads.map(d=>d.date), gV=grads.map(d=>(+d.count||0));
        drawBarChart(el('chart-grad'), gL, gV, chartColors);
        const gradDist=data.graduationByDifficulty||{}; const gt=el('grad-total');
        if(gt){ gt.textContent=`상 ${gradDist.HIGH||0} / 중 ${gradDist.MEDIUM||0} / 하 ${gradDist.LOW||0}`; }
        const tbl=el('tbl-grad'); if(tbl){ tbl.innerHTML=''; (data.graduatedProblems||[]).forEach(p=>{ const tr=document.createElement('tr'); const diff=diffMap[p.difficulty]||p.difficulty; tr.innerHTML=`<td>${p.name}</td><td>${diff}</td>`; tbl.appendChild(tr); }); }

        heatmapData = new Map();
        (data.heatmap || []).forEach(({date, count}) => heatmapData.set(date, count));
        renderMonthlyHeatmap();
    } catch(e){
        toast('대시보드 로드 실패: '+e.message, 'bad');
        renderMonthlyHeatmap();
    }
}

function drawBarChart(canvas, labels, values, colors){
    const ctx = canvas.getContext('2d'); const w=canvas.width,h=canvas.height;
    ctx.clearRect(0,0,w,h);
    const pad=28,bw=(w-pad*2)/Math.max(1,values.length);
    const max=Math.max(1,Math.max(0,...values));
    ctx.strokeStyle = colors.grid; ctx.lineWidth=1;
    ctx.font='12px ui-sans-serif'; ctx.fillStyle=colors.label;
    for(let i=0;i<=4;i++){ const y=pad+(h-pad*2)*(i/4); ctx.beginPath(); ctx.moveTo(pad,y); ctx.lineTo(w-pad,y); ctx.stroke();
        const val=Math.round(max*(1-i/4)); ctx.fillText(String(val),4,y+4); }
    for(let i=0;i<values.length;i++){ const v=values[i]; const bh=(h-pad*2)*(v/max);
        const x=pad+i*bw+4, y=h-pad-bh; ctx.fillStyle=colors.bar; ctx.fillRect(x,y,Math.max(2,bw-8),bh); }
    ctx.fillStyle=colors.label; const step=Math.max(1,Math.ceil(labels.length/10));
    for(let i=0;i<labels.length;i+=step){ const x=pad+i*bw+4; ctx.fillText(labels[i].slice(5),x,h-6); }
}

// ---- Event Listeners & Init ----
function init() {
    const storedTheme = localStorage.getItem('theme') || 'dark';
    applyTheme(storedTheme);

    el('btn-theme')?.addEventListener('click', toggleTheme);
    el('btn-add')?.addEventListener('click', addProblem);
    el('btn-search')?.addEventListener('click', performSearch);
    el('quick-solve')?.addEventListener('click', () => quickAction('solve'));
    el('quick-fail')?.addEventListener('click', () => quickAction('fail'));
    el('btn-refresh-dashboard')?.addEventListener('click', loadDashboard);

    el('p-name').addEventListener('keydown', (e) => { if (e.key === 'Enter') addProblem(); });
    el('s-q').addEventListener('keydown', (e) => { if (e.key === 'Enter') performSearch(); });

    el('heatmap-prev').addEventListener('click', () => {
        heatmapDate.setMonth(heatmapDate.getMonth() - 1);
        renderMonthlyHeatmap();
    });
    el('heatmap-next').addEventListener('click', () => {
        heatmapDate.setMonth(heatmapDate.getMonth() + 1);
        renderMonthlyHeatmap();
    });

    ['s-from','s-to'].forEach(id=>{
        const input=el(id); if(!input) return;
        const open=()=>{ if(typeof input.showPicker==='function') input.showPicker(); };
        input.addEventListener('click',open); input.addEventListener('focus',open);
    });

    Promise.all([loadToday(), performSearch()]).then(loadDashboard);
}

// === Let's GO! ===
init();