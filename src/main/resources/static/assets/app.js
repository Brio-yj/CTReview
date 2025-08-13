
// ================== 콘솔 로거 시스템 START ==================
/**
 * 브라우저 콘솔에 상태별로 색상을 입혀 로그를 출력합니다.
 * @param {string} message - 로그 메시지
 * @param {'SUCCESS'|'FAIL'|'INFO'} status - 로그 상태
 */
function addLog(message, status = 'INFO') {
    const timestamp = new Date().toLocaleTimeString('ko-KR', { hour12: false });
    const logMessage = `[${timestamp}] ${message}`;

    switch (status) {
        case 'SUCCESS':
            console.log(`%c✔ SUCCESS: ${logMessage}`, 'color: #28a745;');
            break;
        case 'FAIL':
            console.error(`✖ FAIL: ${logMessage}`);
            break;
        case 'INFO':
        default:
            console.info(`ℹ️ INFO: ${logMessage}`);
            break;
    }
}
// ================== 콘솔 로거 시스템 END ==================


// ================== API & UTILS ==================
const API = {
    add: () => `/api/problems`,
    today: () => `/api/reviews/today`,
    solveAny: (params) => `/api/problems/solve?${params.toString()}`,
    failAny:  (params) => `/api/problems/fail?${params.toString()}`,
    graduateAny:(params) => `/api/problems/graduate?${params.toString()}`,
    deleteAny:(params) => `/api/problems?${params.toString()}`,
    search:   (params) => `/api/problems?${params.toString()}`,
    dashboard:() => `/api/dashboard/summary`,
    auth: {
        me: () => `/api/auth/me`,
        login: () => `/api/auth/login`,
        register: () => `/api/auth/register`,
        logout: () => `/api/auth/logout`
    }
};

async function http(method, url, body) {
    addLog(`Requesting ${method} ${url}`, 'INFO');
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body !== undefined && body !== null) opts.body = JSON.stringify(body);

    try {
        const res = await fetch(url, opts);
        if (!res.ok) {
            let msg = `${res.status} ${res.statusText}`;
            try {
                const j = await res.json();
                msg = j.message || JSON.stringify(j);
            } catch (e) {
                // JSON 파싱 실패
            }
            addLog(`${method} ${url} -> ${res.status} FAILED: ${msg}`, 'FAIL');
            throw new Error(msg);
        }

        addLog(`${method} ${url} -> ${res.status} SUCCESS`, 'SUCCESS');

        if (res.status === 204) return null;
        return res.json();
    } catch (error) {
        if (!error.message.includes(url)) {
            addLog(`Network request failed for ${method} ${url}`, 'FAIL');
        }
        throw error;
    }
}
const el = (id) => document.getElementById(id);
function fmtDate(d){
    if(!d) return '-';
    const [date,time] = d.split('T');
    if(time){ return `${date.replace(/-/g,'.')} ${time.slice(0,5)}`; }
    return date.replace(/-/g,'.');
}
const diffMap = {HIGH:'상', MEDIUM:'중', LOW:'하'};

let currentUser = null;
async function checkAuth(){
    try{
        currentUser = await http('GET', API.auth.me());
    }catch{ currentUser = null; }
    updateAuthUI();
}
function updateAuthUI(){
    const login = el('btn-login');
    const reg = el('btn-register');
    const logout = el('btn-logout');
    const user = el('auth-user');
    if(currentUser){
        user.textContent = currentUser.email;
        login.style.display = 'none';
        reg.style.display = 'none';
        logout.style.display = '';
    }else{
        user.textContent = '';
        login.style.display = '';
        reg.style.display = '';
        logout.style.display = 'none';
    }
}
async function doLogin(){
    const email = prompt('이메일?');
    if(!email) return;
    const password = prompt('비밀번호?');
    if(password==null) return;
    await http('POST', API.auth.login(), {email, password});
    toast('로그인 완료');
    await checkAuth();
}
async function doRegister(){
    const email = prompt('이메일?');
    if(!email) return;
    const password = prompt('비밀번호?');
    if(password==null) return;
    await http('POST', API.auth.register(), {email, password});
    toast('가입 완료');
}
async function doLogout(){
    await http('POST', API.auth.logout());
    toast('로그아웃');
    await checkAuth();
}

// ================== CORE LOGIC ==================

// ---- 테마 변경 ----
function applyTheme(theme){
    document.body.classList.toggle('light', theme === 'light');
    const icon = el('theme-icon');
    const label = el('theme-label');
    if(icon && label){
        if(theme === 'light'){
            icon.textContent='☀️';
            label.textContent='Light';
        }else{
            icon.textContent='🌙';
            label.textContent='Dark';
        }
    }
    loadDashboard();
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


// assets/app.js 파일의 createProblemRow 함수 전체를 아래 코드로 완전히 교체해 주세요.

function createProblemRow(p, type) {
    const tr = document.createElement('tr');
    tr.classList.add('has-name-tooltip');
    tr.dataset.fullName = p.name;
    if (p.category) {
        tr.dataset.category = p.category;
    }

    const diffTxt = diffMap[p.difficulty] || p.difficulty;

    if (type === 'today') {
        // [수정] 모든 HTML을 하나의 문자열로 만들어 innerHTML에 한 번만 할당합니다.
        tr.innerHTML = `
            <td>${p.category ?? '-'}</td>
            <td>${p.number ?? '-'}</td>
            <td>${p.name}</td>
            <td>${diffTxt}</td>
            <td>${p.reviewStep}</td>
            <td>
                <div class="row" style="gap: 4px;">
                    <button class="btn btn-ok" data-act="solve">Solve</button>
                    <button class="btn btn-bad" data-act="fail">Fail</button>
                </div>
            </td>
        `;

        // 이벤트 리스너는 innerHTML 할당 후에 요소(element)를 찾아서 직접 연결합니다.
        const solveBtn = tr.querySelector('[data-act="solve"]');
        const failBtn = tr.querySelector('[data-act="fail"]');
        solveBtn.addEventListener('click', () => { actBy('solve', p, solveBtn, failBtn); });
        failBtn.addEventListener('click', () => { actBy('fail', p, solveBtn, failBtn); });

        return tr;
    }

    // === type === 'search' ===
    // 검색 결과에도 has-name-tooltip 클래스가 적용되어 클릭 툴팁이 작동합니다
    const graduateButtonHtml = p.status !== 'GRADUATED'
        ? `<button class="btn" data-act="graduate">졸업</button>`
        : '✔️';

    tr.innerHTML = `
        <td>${p.name}</td>
        <td>${diffTxt}</td>
        <td>${fmtDate(p.nextReviewDate)}</td>
        <td>${p.reviewStep}</td>
        <td>${graduateButtonHtml}</td>
        <td><button class="btn" data-act="delete">삭제</button></td>
    `;

    const gradBtn = tr.querySelector('[data-act="graduate"]');
    if (gradBtn) {
        gradBtn.addEventListener('click', () => { actBy('graduate', p, gradBtn); });
    }
    tr.querySelector('[data-act="delete"]').addEventListener('click', (e) => {
        delBy(p, e.target);
    });

    return tr;
}


/* assets/app.js 파일의 performSearch 함수 내부의 'colspan' 값을 수정합니다. */
async function performSearch(){
    const tbody = el('tbl-search');
    try{
        const params = new URLSearchParams();
        const n = el('s-number')?.value; const q = el('s-q')?.value?.trim();
        const diff = el('s-difficulty')?.value;  const from = el('s-from')?.value; const to = el('s-to')?.value;
        const sort = el('s-sort')?.value;
        if (n) params.set('number', n);
        if (q) params.set('q', q);
        if (diff) params.set('difficulty', diff);
        if (from) params.set('from', from);
        if (to) params.set('to', to);
        if (sort) params.set('sort', sort);

        const list = await http('GET', API.search(params));
        tbody.innerHTML='';
        if (!list || !list.length){
            tbody.innerHTML = `<tr><td colspan="6" style="color:var(--muted)">검색 결과가 없습니다.</td></tr>`;
        } else {
            list.forEach(p => tbody.appendChild(createProblemRow(p, 'search')));
        }
    } catch(e){
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--bad)">검색 실패: ${e.message}</td></tr>`;
    }
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
        const gradDist=data.graduationByDifficulty||{}; const gt=el('grad-total');
        if(gt){ gt.textContent=`상 ${gradDist.HIGH||0} / 중 ${gradDist.MEDIUM||0} / 하 ${gradDist.LOW||0}`; }

        const gradListContainer = el('grad-list');
        if(gradListContainer){
            gradListContainer.innerHTML = ''; // 컨테이너 비우기
            (data.graduatedProblems||[]).forEach(p => {
                const pill = document.createElement('div');
                pill.className = `pill-grad ${p.difficulty}`;
                pill.textContent = p.name;
                pill.title = `난이도: ${diffMap[p.difficulty] || p.difficulty}`;
                gradListContainer.appendChild(pill);
            });
        }

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
    el('btn-login')?.addEventListener('click', doLogin);
    el('btn-register')?.addEventListener('click', doRegister);
    el('btn-logout')?.addEventListener('click', doLogout);

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

    checkAuth();
    Promise.all([loadToday(), performSearch()]).then(loadDashboard);

    // ================== 이름 툴팁 시스템 (클릭 방식) START ==================
    // 1. 툴팁으로 사용할 div 요소를 body에 딱 한 번만 추가합니다.
    const tooltip = document.createElement('div');
    tooltip.id = 'name-tooltip';
    document.body.appendChild(tooltip);

    // 2. 문서 전체의 클릭 이벤트를 감지하여 툴팁을 제어합니다.
    document.addEventListener('click', (e) => {
        const clickedTr = e.target.closest('tr.has-name-tooltip');
        const currentSelectedTr = document.querySelector('tr.selected');
        const tooltip = document.getElementById('name-tooltip');

        // 툴팁을 클릭한 경우 아무것도 하지 않음 (툴팁이 사라지지 않게)
        if (e.target.closest('#name-tooltip')) {
            return;
        }

        // 먼저, 이전에 선택됐던 행이 있다면 선택 해제합니다.
        if (currentSelectedTr) {
            currentSelectedTr.classList.remove('selected');
        }

        // 어떤 곳을 클릭하든 일단 툴팁은 숨깁니다.
        tooltip.style.display = 'none';

        // 만약 클릭한 곳이 유효한 테이블 행(tr)이라면,
        if (clickedTr) {
            // 그리고 이전에 선택했던 행이 아닌 새로운 행을 클릭했다면,
            if (clickedTr !== currentSelectedTr) {
                clickedTr.classList.add('selected'); // 새로운 행에 'selected' 클래스를 추가합니다.

                // 툴팁 내용 구성
                const fullName = clickedTr.dataset.fullName;
                const category = clickedTr.dataset.category;
                let tooltipText = `문제이름: ${fullName}`;
                if (category) {
                    tooltipText = `카테고리: ${category}, ${tooltipText}`;
                }
                tooltip.textContent = tooltipText;

                // 툴팁 위치를 클릭한 마우스 위치 기준으로 설정하고 보여줍니다.
                tooltip.style.left = `${e.pageX + 10}px`;
                tooltip.style.top = `${e.pageY + 10}px`;
                tooltip.style.display = 'block';
            }
            // 만약 이전에 선택했던 행을 다시 클릭했다면,
            // 위 로직에 따라 선택이 해제되고 툴팁도 숨겨진 상태로 유지됩니다. (토글 효과)
        }
    });
    // ================== 이름 툴팁 시스템 (클릭 방식) END ==================
    // 툴팁 내의 텍스트를 더블클릭하면 전체 선택되도록 추가 기능
    document.addEventListener('dblclick', (e) => {
        if (e.target.closest('#name-tooltip')) {
            // 툴팁 내용 전체 선택
            const tooltip = document.getElementById('name-tooltip');
            const selection = window.getSelection();
            const range = document.createRange();
            range.selectNodeContents(tooltip);
            selection.removeAllRanges();
            selection.addRange(range);
        }
    });
}

// === Let's GO! ===
init();