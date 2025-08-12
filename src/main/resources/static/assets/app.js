// ================== API & UTILS ==================
const API = {
    add: () => `/api/problems`,
    today: () => `/api/reviews/today`,
    solveAny: (params) => `/api/problems/solve?${params.toString()}`,
    failAny:  (params) => `/api/problems/fail?${params.toString()}`,
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
function fmtDate(d){ return d ?? '-'; }

// ================== CORE LOGIC ==================

// ---- 3. 새로고침 없는 테마 변경 ----
function applyTheme(theme){
    document.body.classList.toggle('light', theme === 'light');
    const btn = el('btn-theme');
    if (btn) btn.textContent = theme === 'light' ? '다크 모드' : '라이트 모드';
    // 테마 변경 시 대시보드(차트, 히트맵) 다시 그리기
    loadDashboard();
}
function toggleTheme(){
    const currentTheme = document.body.classList.contains('light') ? 'light' : 'dark';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    localStorage.setItem('theme', newTheme);
    applyTheme(newTheme);
}

// ---- Toast (CSS 기반으로 변경) ----
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
    // type에 따라 info, ok, bad 클래스 추가
    div.className = `toast ${type}`;
    div.textContent = msg;
    toastHost().appendChild(div);
    setTimeout(()=>div.remove(), 2200);
}

// ---- Data Load & Render ----
function createActionButtons(problem, type) {
    const tpl = el('row-actions');
    const node = tpl.content.cloneNode(true);
    const [btnSolve, btnFail, btnDelete] = node.querySelectorAll('button');

    btnSolve.addEventListener('click', ()=> actBy('solve', problem, btnSolve, btnFail, btnDelete));
    btnFail.addEventListener('click', ()=> actBy('fail',  problem, btnSolve, btnFail, btnDelete));
    btnDelete.addEventListener('click',()=> delBy(problem, btnSolve, btnFail, btnDelete));

    // 1. 검색 결과에서는 Solve/Fail 버튼 숨기기
    if (type === 'search') {
        btnSolve.style.display = 'none';
        btnFail.style.display = 'none';
    }
    return node;
}

async function loadToday(){
    const tbody = el('tbl-today');
    try {
        const list = await http('GET', API.today());
        tbody.innerHTML='';
        if (!list || !list.length){
            tbody.innerHTML = `<tr><td colspan="7" style="color:var(--muted)">오늘 복습할 문제가 없습니다.</td></tr>`;
        } else {
            list.forEach(p => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${p.category ?? '-'}</td>
                    <td>${p.number ?? '-'}</td>
                    <td>${p.name}</td>
                    <td><code class="badge">LV.${p.currentLevel}</code></td>
                    <td>${p.reviewCount}</td>
                    <td>${fmtDate(p.nextReviewDate)}</td>
                    <td></td>`;
                tr.children[6].appendChild(createActionButtons(p, 'today'));
                tbody.appendChild(tr);
            });
        }
    } catch(e){
        tbody.innerHTML = `<tr><td colspan="7" style="color:var(--bad)">오늘 목록 로드 실패: ${e.message}</td></tr>`;
    }
}

async function performSearch(){
    const tbody = el('tbl-search');
    try{
        const params = new URLSearchParams();
        const n = el('s-number')?.value; const q = el('s-q')?.value?.trim();
        const lv = el('s-level')?.value;  const from = el('s-from')?.value; const to = el('s-to')?.value;
        const sort = el('s-sort')?.value;
        if (n) params.set('number', n);
        if (q) params.set('q', q);
        if (lv) params.set('level', lv);
        if (from) params.set('from', from);
        if (to) params.set('to', to);
        if (sort) params.set('sort', sort);

        const list = await http('GET', API.search(params));
        tbody.innerHTML='';
        if (!list || !list.length){
            tbody.innerHTML = `<tr><td colspan="7" style="color:var(--muted)">검색 결과가 없습니다.</td></tr>`;
        } else {
            list.forEach(p => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${p.category ?? '-'}</td>
                    <td>${p.number ?? '-'}</td>
                    <td>${p.name}</td>
                    <td><code class="badge">LV.${p.currentLevel}</code></td>
                    <td>${p.reviewCount}</td>
                    <td>${fmtDate(p.nextReviewDate)}</td>
                    <td></td>`;
                tr.children[6].appendChild(createActionButtons(p, 'search')); // type: 'search' 전달
                tbody.appendChild(tr);
            });
        }
    } catch(e){
        tbody.innerHTML = `<tr><td colspan="7" style="color:var(--bad)">검색 실패: ${e.message}</td></tr>`;
    }
}

// ---- Actions ----
async function addProblem() {
    const numVal = el('p-number').value; const number = numVal ? parseInt(numVal,10) : null;
    const name = el('p-name').value.trim(); const category = el('p-category')?.value || null;
    const level = parseInt(el('p-level').value,10);
    if (!name) return toast('문제 이름을 입력해 주세요', 'bad');
    const payload = { name, level };
    if (number !== null && !Number.isNaN(number)) payload.number = number;
    if (category) payload.category = category;
    try {
        await http('POST', API.add(), payload);
        el('p-number').value=''; el('p-name').value=''; el('p-level').value='3';
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
        const url = (kind==='solve') ? API.solveAny(params) : API.failAny(params);
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


// ---- Dashboard & Heatmap (테마 연동) ----
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

function renderWeeklyHeatmap(dailyCounts){
    const grid = el('heatmap-grid');
    if(!grid) return;
    grid.innerHTML='';
    const map = new Map(); let maxVal = 0;
    (dailyCounts||[]).forEach(({date,count})=>{ map.set(date,count); if(count>maxVal)maxVal=count; });
    const today = new Date();
    const firstDate = dailyCounts && dailyCounts.length ? new Date(dailyCounts[0].date) : today;
    const start = new Date(firstDate);
    start.setDate(start.getDate() - start.getDay());
    const totalDays = Math.floor((today - start) / (1000*60*60*24)) + 1;
    const weeks = Math.ceil(totalDays / 7);

    for(let w=0; w<weeks; w++){
        for(let d=0; d<7; d++){
            const cellDate=new Date(start.getFullYear(),start.getMonth(),start.getDate()+(w*7+d));
            const key = cellDate.toISOString().slice(0,10);
            const val = map.get(key)??0;
            const level = (val === 0) ? 0 : Math.min(4, Math.ceil(val / (Math.max(1, maxVal) / 4)));
            const cell = document.createElement('div');
            cell.className='cell';
            cell.dataset.level = String(level);
            cell.setAttribute('title',`${key}: ${val}`);
            grid.appendChild(cell);
        }
    }
    const wrapper = grid.parentElement;
    if(wrapper) wrapper.scrollLeft = wrapper.scrollWidth;
}

async function loadDashboard(){
    try{
        const data = await http('GET', API.dashboard());
        el('streak').textContent=`연속일: ${data.streak}일`;
        const todayEl=el('today-text'); if(todayEl) todayEl.textContent=data.today;
        const dist=data.levelDistribution||{}; const box=el('level-dist');
        if(box){ box.innerHTML=''; [3,2,1,0].forEach(l=>{ const v=dist[l]||0; if(v>0){const row=document.createElement('div'); row.innerHTML=`<code class="badge">LV.${l}</code> × ${v}`; box.appendChild(row); }});}

        const isLight = document.body.classList.contains('light');
        const style = getComputedStyle(document.body);
        const chartColors = {
            grid: isLight ? style.getPropertyValue('--chart-grid') : '#27345f',
            label: isLight ? style.getPropertyValue('--chart-label') : '#8aa0d9',
            bar: isLight ? style.getPropertyValue('--chart-bar') : '#4f7cff'
        };

        const daily=Array.isArray(data.daily)?data.daily:[]; const dL=daily.map(d=>d.date), dV=daily.map(d=>(+d.count||0));
        drawBarChart(el('chart-daily'), dL, dV, chartColors);
        const grads=Array.isArray(data.graduations)?data.graduations:[]; const gL=grads.map(d=>d.date), gV=grads.map(d=>(+d.count||0));
        drawBarChart(el('chart-grad'), gL, gV, chartColors); const gt=el('grad-total'); if(gt) gt.textContent=`${gV.reduce((a,b)=>a+b,0)}`;

        renderWeeklyHeatmap(data.heatmap);
    } catch(e){
        toast('대시보드 로드 실패: '+e.message, 'bad');
        renderWeeklyHeatmap([]);
    }
}


// ---- Event Listeners & Init ----
function init() {
    // 테마 초기화
    const storedTheme = localStorage.getItem('theme') || 'dark';
    applyTheme(storedTheme);

    // 버튼 이벤트 리스너
    el('btn-theme')?.addEventListener('click', toggleTheme);
    el('btn-add')?.addEventListener('click', addProblem);
    el('btn-refresh-today')?.addEventListener('click', loadToday);
    el('btn-search')?.addEventListener('click', performSearch);
    el('quick-solve')?.addEventListener('click', () => quickAction('solve'));
    el('quick-fail')?.addEventListener('click', () => quickAction('fail'));
    el('btn-refresh-dashboard')?.addEventListener('click', loadDashboard);

    // Date input UX
    ['s-from','s-to'].forEach(id=>{
        const input=el(id); if(!input) return;
        const open=()=>{ if(typeof input.showPicker==='function') input.showPicker(); };
        input.addEventListener('click',open); input.addEventListener('focus',open);
    });

    // 초기 데이터 로드
    Promise.all([loadToday(), performSearch()]).then(loadDashboard);
}

// === Let's GO! ===
init();