const API_BASE = "http://localhost:8080/api";
let currentUser = { id: null, type: null, name: null }; // type: 'USER' or 'ADMIN'
let activeEventsMap = {};
function showToast(message, type = "info") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
async function fetchAPI(endpoint, options = {}) {
    const headers = { "Content-Type": "application/json", ...options.headers };
    if (currentUser.id) {
        if (currentUser.type === 'ADMIN') headers["X-Admin-Id"] = currentUser.id;
        if (currentUser.type === 'USER') headers["X-User-Id"] = currentUser.id;
    }
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
        let data;
        const text = await response.text();
        try { data = JSON.parse(text); } catch { data = text; }
        if (!response.ok) {
            if (response.status === 400 || response.status === 403 || response.status === 409 || response.status === 401) {
                showToast(typeof data === 'string' ? data : data.message || "Error occurred", "error");
            } else {
                showToast("Server error: " + response.status, "error");
            }
            throw new Error(data);
        }
        return data;
    } catch (error) {
        console.error("API Error:", error);
        throw error;
    }
}
const el = (id) => document.getElementById(id);
const hide = (id) => el(id).classList.add("hidden");
const show = (id) => el(id).classList.remove("hidden");
const sortByDateDesc = (a, b) => {
    if (!a.createdAt) return 1;
    if (!b.createdAt) return -1;
    return new Date(b.createdAt.replace(' ', 'T')) - new Date(a.createdAt.replace(' ', 'T'));
};
const fDate = (d) => d ? `[${d}] ` : "";
function init() {
    const savedId = localStorage.getItem("p2p_id");
    const savedType = localStorage.getItem("p2p_type");
    const savedName = localStorage.getItem("p2p_name");
    if (savedId && savedType) {
        currentUser = { id: savedId, type: savedType, name: savedName };
        renderDashboard();
    } else {
        renderAuth();
    }
    setupEventListeners();
}
function renderAuth() {
    hide("navbar");
    hide("user-dashboard");
    hide("admin-dashboard");
    show("auth-section");
}
function renderDashboard() {
    hide("auth-section");
    show("navbar");
    el("display-username").textContent = currentUser.name + (currentUser.type === 'ADMIN' ? ' (Admin)' : '');
    populateDropdowns();
    if (currentUser.type === 'USER') {
        show("user-dashboard");
        hide("admin-dashboard");
        show("nav-wallet");
        refreshUserData();
    } else {
        show("admin-dashboard");
        hide("user-dashboard");
        hide("nav-wallet");
        fetchAPI("/events/available").then(renderEvents).catch(e => console.log(e));
        refreshAdminAudit();
    }
}
function logout() {
    localStorage.clear();
    currentUser = { id: null, type: null, name: null };
    renderAuth();
    showToast("Logged out successfully", "success");
}
async function populateDropdowns() {
    try {
        if (currentUser.type === 'USER') {
            const availableEvents = await fetchAPI("/events/available");
            activeEventsMap = {};
            const betEventSelect = el("bet-event-id");
            if (betEventSelect) {
                betEventSelect.innerHTML = '<option value="">Select Event</option>';
                availableEvents.forEach(ev => {
                    activeEventsMap[ev.id] = ev;
                    betEventSelect.innerHTML += `<option value="${ev.id}">${ev.id} - ${ev.description}</option>`;
                });
            }
        } else if (currentUser.type === 'ADMIN') {
            const availableEvents = await fetchAPI("/events/available");
            const allEvents = await fetchAPI("/events");
            const allUsers = await fetchAPI("/admins/users");
            const availableRooms = await fetchAPI("/gamerooms/available");
            const startEventSelect = el("admin-start-event-id");
            if (startEventSelect) {
                startEventSelect.innerHTML = '<option value="">Select Event</option>';
                availableEvents.forEach(ev => startEventSelect.innerHTML += `<option value="${ev.id}">${ev.id} - ${ev.description}</option>`);
            }
            const resultEventSelect = el("admin-result-event-id");
            if (resultEventSelect) {
                resultEventSelect.innerHTML = '<option value="">Select Event</option>';
                const inProgressEvents = allEvents.filter(e => e.status === 'IN_PROGRESS');
                inProgressEvents.forEach(ev => resultEventSelect.innerHTML += `<option value="${ev.id}">${ev.id} - ${ev.description}</option>`);
            }
            const walletUserSelect = el("admin-wallet-user-id");
            if (walletUserSelect) {
                walletUserSelect.innerHTML = '<option value="">Select User</option>';
                allUsers.forEach(u => walletUserSelect.innerHTML += `<option value="${u.id}">${u.username} (ID: ${u.id})</option>`);
            }
            const historyUserSelect = el("admin-history-user-id");
            if (historyUserSelect) {
                historyUserSelect.innerHTML = '<option value="">Select User</option>';
                allUsers.forEach(u => historyUserSelect.innerHTML += `<option value="${u.id}">${u.username} (ID: ${u.id})</option>`);
            }
            const cancelRoomSelect = el("admin-cancel-room-id");
            if (cancelRoomSelect) {
                cancelRoomSelect.innerHTML = '<option value="">Select Room</option>';
                const openRooms = availableRooms.filter(r => r.status === 'OPEN');
                openRooms.forEach(r => cancelRoomSelect.innerHTML += `<option value="${r.roomId}">Room #${r.roomId} (${r.gameType})</option>`);
            }
        }
    } catch (e) {
        console.error("Error populating dropdowns", e);
    }
}
async function refreshAdminAudit() {
    if (currentUser.type !== 'ADMIN') return;
    try {
        const events = await fetchAPI("/events");
        const bets = await fetchAPI("/admins/bets");
        const rooms = await fetchAPI("/gamerooms");
        events.sort(sortByDateDesc);
        bets.sort(sortByDateDesc);
        rooms.sort(sortByDateDesc);
        const eventsList = el("admin-all-events-list");
        if (eventsList) {
            eventsList.innerHTML = "";
            if (events.length === 0) eventsList.innerHTML = "<li>No events found</li>";
            events.forEach(ev => {
                let winnerStr = (ev.status === 'FINISHED' && ev.finalResult) ? ` | <strong class="text-success">Winner: ${ev.finalResult}</strong>` : "";
                eventsList.innerHTML += `<li>${fDate(ev.createdAt)}Event #${ev.id} | Status: ${ev.status} | ${ev.description}${winnerStr}</li>`;
            });
        }
        const betsList = el("admin-all-bets-list");
        if (betsList) {
            betsList.innerHTML = "";
            if (bets.length === 0) betsList.innerHTML = "<li>No bets found</li>";
            bets.forEach(b => {
                let winnerStr = "";
                if (b.status === "SETTLED") {
                    const ev = events.find(e => e.id === b.eventId);
                    if (ev && ev.finalResult) {
                        const creatorWon = ev.finalResult === b.creatorSelection;
                        winnerStr = creatorWon 
                            ? ` | <strong class="text-success">Winner: Creator (ID: ${b.creatorId})</strong>` 
                            : ` | <strong class="text-success">Winner: Acceptor (ID: ${b.acceptorId})</strong>`;
                    }
                } else if (b.status === "CANCELLED") {
                    winnerStr = ` | <strong>REFUNDED</strong>`;
                }
                betsList.innerHTML += `<li>${fDate(b.createdAt)}Bet #${b.id} | Event: ${b.eventId} | Status: ${b.status} | Stake: $${b.stake} | Choice: ${b.creatorSelection}${winnerStr}</li>`;
            });
        }
        const roomsList = el("admin-all-rooms-list");
        if (roomsList) {
            roomsList.innerHTML = "";
            if (rooms.length === 0) roomsList.innerHTML = "<li>No rooms found</li>";
            rooms.forEach(r => {
                let winnerStr = "";
                if (r.status === "FINISHED") {
                    if (r.winnerId === -1) winnerStr = ` | <strong>REFUNDED</strong>`;
                    else winnerStr = ` | <strong class="text-success">Winner ID: ${r.winnerId}</strong>`;
                }
                roomsList.innerHTML += `<li>${fDate(r.createdAt)}Room #${r.roomId} (${r.gameType}) | Status: ${r.status} | Pool: $${r.poolAmount}${winnerStr}</li>`;
            });
        }
        const activeRoomsList = el("admin-active-rooms-list");
        if (activeRoomsList) {
            activeRoomsList.innerHTML = "";
            const activeRooms = rooms.filter(r => r.status === 'OPEN' || r.status === 'IN_PROGRESS');
            if (activeRooms.length === 0) activeRoomsList.innerHTML = "<li>No active rooms found</li>";
            activeRooms.forEach(r => {
                activeRoomsList.innerHTML += `<li>${fDate(r.createdAt)}Room #${r.roomId} (${r.gameType}) | Status: ${r.status} | Players: ${r.participants.length}/${r.capacity}</li>`;
            });
        }
    } catch (e) {
        console.error("Error refreshing admin audit", e);
    }
}
async function refreshUserData() {
    try {
        const wallet = await fetchAPI(`/wallets/${currentUser.id}/details`);
        if(el("nav-wallet-balance")) el("nav-wallet-balance").textContent = parseFloat(wallet.availableBalance).toFixed(2);
        if(el("nav-wallet-locked")) el("nav-wallet-locked").textContent = parseFloat(wallet.lockedBalance).toFixed(2);
        const bets = await fetchAPI("/bets/available");
        renderBets(bets);
        const rooms = await fetchAPI("/gamerooms/available");
        renderRooms(rooms);
        const events = await fetchAPI("/events/available");
        renderEvents(events);
        const allEvents = await fetchAPI("/events");
        allEvents.forEach(ev => activeEventsMap[ev.id] = ev);
        const history = await fetchAPI(`/history/${currentUser.id}`);
        renderHistory(history);
        renderPendingBets(history.bets);
    } catch (e) {
    }
}
function renderPendingBets(bets) {
    const container = el("my-pending-bets-list");
    if (!container) return;
    container.innerHTML = "";
    let pendingBets = bets.filter(b => b.status === 'PENDING' || b.status === 'MATCHED');
    pendingBets.sort(sortByDateDesc);
    if (pendingBets.length === 0) {
        container.innerHTML = "<p class='item-sub'>No active bets.</p>";
        return;
    }
    pendingBets.forEach(bet => {
        const ev = activeEventsMap[bet.eventId];
        const opponentOption = ev ? (bet.creatorSelection === ev.optionA ? ev.optionB : ev.optionA) : 'Unknown';
        const isCreator = bet.creatorId == currentUser.id;
        const myPick = isCreator ? bet.creatorSelection : opponentOption;
        const theirPick = isCreator ? opponentOption : bet.creatorSelection;
        const div = document.createElement("div");
        div.className = "list-item";
        div.innerHTML = `
            <div>
                <div class="item-title">${fDate(bet.createdAt)}Bet #${bet.id} <span style="color:var(--accent-color)">[${bet.status}]</span> (Event: ${bet.eventId})</div>
                <div class="item-sub">Stake: $${bet.stake} | Your Pick: ${myPick} (vs ${theirPick})</div>
            </div>
            ${bet.status === 'PENDING' ? `<button class="btn btn-outline btn-sm" onclick="cancelBet(${bet.id})">Cancel</button>` : ''}
        `;
        container.appendChild(div);
    });
}
window.cancelBet = async function(betId) {
    try {
        await fetchAPI(`/bets/${betId}/cancel?requesterId=${currentUser.id}`, { method: 'PUT' });
        showToast("Bet cancelled successfully", "success");
        refreshUserData();
    } catch (e) { }
}
function renderBets(bets) {
    const container = el("available-bets-list");
    container.innerHTML = "";
    bets.sort(sortByDateDesc);
    if (bets.length === 0) {
        container.innerHTML = "<p class='item-sub'>No available bets right now.</p>";
        return;
    }
    bets.forEach(bet => {
        if (bet.creatorId == currentUser.id) return; // Don't show own bets to accept
        const ev = activeEventsMap[bet.eventId];
        if (!ev) return; // Event not found in active events
        const acceptorOption = bet.creatorSelection === ev.optionA ? ev.optionB : ev.optionA;
        const div = document.createElement("div");
        div.className = "list-item";
        div.innerHTML = `
            <div class="item-info">
                <span class="item-title">${fDate(bet.createdAt)}Event: ${bet.eventId} | Stake: $${bet.stake}</span>
                <span class="item-sub">Creator chose: ${bet.creatorSelection}. Accept to bet on: <strong>${acceptorOption}</strong></span>
            </div>
            <button class="btn btn-primary btn-sm" onclick="acceptBet(${bet.id})">Accept (${acceptorOption})</button>
        `;
        container.appendChild(div);
    });
}
function renderRooms(rooms) {
    const container = el("available-rooms-list");
    container.innerHTML = "";
    rooms.sort(sortByDateDesc);
    if (rooms.length === 0) {
        container.innerHTML = "<p class='item-sub'>No available rooms right now.</p>";
        return;
    }
    rooms.forEach(room => {
        const isInRoom = room.participants.some(p => p.userId === currentUser.id);
        const div = document.createElement("div");
        div.className = "list-item";
        div.innerHTML = `
            <div class="item-info">
                <span class="item-title">${fDate(room.createdAt)}Room: ${room.roomId} (${room.gameType})</span>
                <span class="item-sub">Fee: $${room.entryFee} | Players: ${room.participants.length}/${room.capacity}</span>
            </div>
            ${isInRoom 
                ? `<button class="btn btn-outline btn-sm" onclick="leaveRoom(${room.roomId})">Leave</button>`
                : `<button class="btn btn-secondary btn-sm" onclick="showJoinPanel(${room.roomId}, '${room.gameType}')">Join</button>`
            }
        `;
        container.appendChild(div);
    });
}
function renderHistory(history) {
    const betsList = el("history-bets");
    const gamesList = el("history-games");
    betsList.innerHTML = "";
    gamesList.innerHTML = "";
    history.bets.sort(sortByDateDesc);
    history.games.sort(sortByDateDesc);
    history.bets.forEach(bet => {
        let outcome = "PENDING";
        if (bet.status === "SETTLED") {
            const ev = activeEventsMap[bet.eventId];
            if (ev && ev.finalResult) {
                const creatorWon = ev.finalResult === bet.creatorSelection;
                if (creatorWon) {
                    outcome = (bet.creatorId == currentUser.id) ? "WON (+" + bet.stake + ")" : "LOST (-" + bet.stake + ")";
                } else {
                    outcome = (bet.acceptorId == currentUser.id) ? "WON (+" + bet.stake + ")" : "LOST (-" + bet.stake + ")";
                }
            } else {
                outcome = "SETTLED";
            }
        } else if (bet.status === "CANCELLED") {
            outcome = "REFUNDED";
        } else if (bet.status === "MATCHED") {
            outcome = "ONGOING";
        }
        let colorClass = outcome.startsWith("WON") ? "text-success" : (outcome.startsWith("LOST") ? "text-danger" : "");
        betsList.innerHTML += `<li>${fDate(bet.createdAt)}Bet #${bet.id} on Event ${bet.eventId} - Stake: $${bet.stake} - <strong class="${colorClass}">${outcome}</strong></li>`;
    });
    history.games.forEach(game => {
        let outcome = "OPEN";
        if (game.status === "FINISHED") {
            if (game.winnerId === currentUser.id) {
                const profit = game.poolAmount - game.entryFee;
                outcome = `WON (+${profit})`;
            } else if (game.winnerId === -1) {
                outcome = "REFUNDED";
            } else {
                outcome = `LOST (-${game.entryFee})`;
            }
        } else if (game.status === "IN_PROGRESS") {
            outcome = "ONGOING";
        }
        let colorClass = outcome.startsWith("WON") ? "text-success" : (outcome.startsWith("LOST") ? "text-danger" : "");
        gamesList.innerHTML += `<li>${fDate(game.createdAt)}Room #${game.roomId} (${game.gameType}) - <strong class="${colorClass}">${outcome}</strong></li>`;
    });
}
function renderEvents(events) {
    const userContainer = el("user-active-events-list");
    const adminContainer = el("admin-active-events-list");
    if(userContainer) userContainer.innerHTML = "";
    if(adminContainer) adminContainer.innerHTML = "";
    events.sort(sortByDateDesc);
    if (events.length === 0) {
        if(userContainer) userContainer.innerHTML = "<p class='item-sub'>No active events right now.</p>";
        if(adminContainer) adminContainer.innerHTML = "<p class='item-sub'>No active events right now.</p>";
        return;
    }
    events.forEach(ev => {
        const html = `
            <div class="list-item">
                <div class="item-info">
                    <span class="item-title">${fDate(ev.createdAt)}Event #${ev.id}: ${ev.description}</span>
                    <span class="item-sub">Options: ${ev.optionA} vs ${ev.optionB} | Status: ${ev.status}</span>
                </div>
            </div>
        `;
        if(userContainer && ev.status !== 'FINISHED') userContainer.innerHTML += html;
        if(adminContainer) adminContainer.innerHTML += html;
    });
}
async function acceptBet(betId) {
    try {
        await fetchAPI("/bets/match", {
            method: "PUT",
            body: JSON.stringify({ betId, acceptorId: currentUser.id })
        });
        showToast("Bet accepted successfully!", "success");
        refreshUserData();
    } catch (e) { }
}
async function showJoinPanel(roomId, gameType) {
    el("join-room-id").value = roomId;
    el("join-room-title").textContent = `Join Room #${roomId} (${gameType})`;
    const choiceSelect = el("join-room-choice");
    choiceSelect.innerHTML = '<option value="">-- Loading Choices --</option>';
    show("join-room-panel");
    try {
        const choices = await fetchAPI(`/gamerooms/${roomId}/choices`);
        if (gameType === "HIGH_CARD" || gameType === "HIGHEST_ROLL" || gameType === "JACKPOT") {
            choiceSelect.style.display = "none";
            choiceSelect.innerHTML = '<option value="AUTO">Auto Assign</option>';
        } else {
            choiceSelect.style.display = "block";
            choiceSelect.innerHTML = '<option value="">-- Pick a Choice --</option>';
            choices.forEach(c => {
                choiceSelect.innerHTML += `<option value="${c}">${c}</option>`;
            });
            if(choices.length === 0) {
                 choiceSelect.innerHTML = '<option value="">-- Room Full --</option>';
            }
        }
    } catch (e) {
        choiceSelect.innerHTML = '<option value="">-- Error --</option>';
    }
}
async function leaveRoom(roomId) {
    try {
        await fetchAPI(`/gamerooms/${roomId}/leave?userId=${currentUser.id}`, { method: "PUT" });
        showToast("Left room successfully. Entry fee refunded.", "success");
        refreshUserData();
    } catch (e) { }
}
function setupEventListeners() {
    document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
            document.querySelectorAll(".auth-content").forEach(c => c.classList.add("hidden"));
            e.target.classList.add("active");
            show(e.target.dataset.target);
        });
    });
    el("logout-btn").addEventListener("click", logout);
    el("btn-user-register").addEventListener("click", async () => {
        const username = el("user-username").value;
        const password = el("user-password").value;
        if (!username || !password) return showToast("Fill all fields", "warning");
        try {
            await fetchAPI("/users/register", { method: "POST", body: JSON.stringify({ username, password }) });
            showToast("Registered successfully. Now login.", "success");
        } catch (e) { }
    });
    el("user-login-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const username = el("user-username").value;
        const password = el("user-password").value;
        try {
            const id = await fetchAPI("/users/login", { method: "POST", body: JSON.stringify({ username, password }) });
            localStorage.setItem("p2p_id", id);
            localStorage.setItem("p2p_type", "USER");
            localStorage.setItem("p2p_name", username);
            currentUser = { id, type: "USER", name: username };
            showToast("Login successful", "success");
            renderDashboard();
        } catch (e) { }
    });
    el("link-forgot-password").addEventListener("click", (e) => {
        e.preventDefault();
        hide("user-auth");
        show("forgot-password-auth");
    });
    el("btn-cancel-forgot").addEventListener("click", () => {
        hide("forgot-password-auth");
        show("user-auth");
    });
    el("forgot-password-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const email = el("forgot-email").value;
        if (!email) return showToast("Enter your email", "warning");
        try {
            const result = await fetchAPI("/users/forgot-password", { 
                method: "POST", 
                body: JSON.stringify({ email }) 
            });
            showToast(result, "success");
            hide("forgot-password-auth");
            show("reset-password-auth");
        } catch (e) { }
    });
    el("btn-cancel-reset").addEventListener("click", () => {
        hide("reset-password-auth");
        show("user-auth");
    });
    el("reset-password-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const token = el("reset-token").value;
        const newPassword = el("reset-new-password").value;
        if (!token || !newPassword) return showToast("Fill all fields", "warning");
        try {
            const result = await fetchAPI("/users/reset-password", {
                method: "POST",
                body: JSON.stringify({ token, newPassword })
            });
            showToast(result, "success");
            hide("reset-password-auth");
            show("user-auth");
            el("user-password").value = ""; // Clear password field just in case
        } catch (e) { }
    });
    el("btn-admin-register").addEventListener("click", async () => {
        const username = el("admin-username").value;
        const password = el("admin-password").value;
        const secret = el("admin-secret").value; // Assuming an input exists if you unhide it
        if (!username || !password) return showToast("Fill all fields", "warning");
        try {
            await fetchAPI("/admins/register", { 
                method: "POST", 
                headers: { "X-Admin-Secret": "SUPER_SECRET_KEY" }, // Hardcoded for demo, normally from input
                body: JSON.stringify({ username, password }) 
            });
            showToast("Admin registered successfully.", "success");
        } catch (e) { }
    });
    el("admin-login-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const username = el("admin-username").value;
        const password = el("admin-password").value;
        try {
            const id = await fetchAPI("/admins/login", { method: "POST", body: JSON.stringify({ username, password }) });
            localStorage.setItem("p2p_id", id);
            localStorage.setItem("p2p_type", "ADMIN");
            localStorage.setItem("p2p_name", username);
            currentUser = { id, type: "ADMIN", name: username };
            showToast("Admin login successful", "success");
            renderDashboard();
        } catch (e) { }
    });
    el("nav-btn-deposit").addEventListener("click", () => {
        el("wallet-modal-title").textContent = "Deposit Funds";
        el("wallet-modal-action").value = "deposit";
        el("wallet-modal-amount").value = "";
        show("wallet-modal");
    });
    el("nav-btn-withdraw").addEventListener("click", () => {
        el("wallet-modal-title").textContent = "Withdraw Funds";
        el("wallet-modal-action").value = "withdraw";
        el("wallet-modal-amount").value = "";
        show("wallet-modal");
    });
    el("btn-modal-cancel").addEventListener("click", () => hide("wallet-modal"));
    el("btn-modal-confirm").addEventListener("click", async () => {
        const action = el("wallet-modal-action").value;
        const amountStr = el("wallet-modal-amount").value.trim();
        const amount = parseFloat(amountStr);
        if (!amountStr || isNaN(amount) || amount <= 0) {
            return showToast("Geçersiz veya boş değer girdiniz", "warning");
        }
        try {
            await fetchAPI(`/wallets/${action}`, { method: "POST", body: JSON.stringify({ userId: currentUser.id, amount }) });
            showToast(`${action === 'deposit' ? 'Deposit' : 'Withdrawal'} successful`, "success");
            hide("wallet-modal");
            refreshUserData();
        } catch (e) { }
    });
    el("bet-event-id").addEventListener("change", (e) => {
        const ev = activeEventsMap[e.target.value];
        const selectionSelect = el("bet-creator-selection");
        selectionSelect.innerHTML = '<option value="">-- Select Option --</option>';
        if (ev && ev.optionA && ev.optionB) {
            selectionSelect.innerHTML += `<option value="${ev.optionA}">${ev.optionA}</option>`;
            selectionSelect.innerHTML += `<option value="${ev.optionB}">${ev.optionB}</option>`;
        }
    });
    el("btn-create-bet").addEventListener("click", async () => {
        const eventIdStr = el("bet-event-id").value.trim();
        const stakeStr = el("bet-stake").value.trim();
        const creatorSelection = el("bet-creator-selection").value.trim();
        const eventId = parseInt(eventIdStr);
        const stake = parseFloat(stakeStr);
        if (!eventIdStr || isNaN(eventId) || eventId <= 0 || !stakeStr || isNaN(stake) || stake <= 0 || !creatorSelection) {
            return showToast("Geçersiz veya boş değer girdiniz", "warning");
        }
        try {
            await fetchAPI("/bets", { 
                method: "POST", 
                body: JSON.stringify({ creatorId: currentUser.id, eventId, stake, creatorSelection }) 
            });
            showToast("Bet created successfully", "success");
            el("bet-stake").value = "";
            el("bet-creator-selection").value = "";
            refreshUserData();
        } catch (e) { }
    });
    function updateCreatorChoices() {
        const type = el("user-room-type").value;
        const capStr = el("user-room-capacity").value;
        const choiceSelect = el("user-room-creator-choice");
        if (!choiceSelect) return;
        choiceSelect.innerHTML = '<option value="">-- Select Your Initial Choice --</option>';
        if (!type || !capStr) return;
        let cap = parseInt(capStr);
        if (isNaN(cap) || cap < 2) return;
        if (type === "DICE_ROULETTE") cap = Math.min(cap, 6);
        if (type === "HIGH_CARD" || type === "HIGHEST_ROLL" || type === "JACKPOT") {
            choiceSelect.style.display = "none";
            choiceSelect.innerHTML = '<option value="AUTO">Auto Assign</option>';
        } else {
            choiceSelect.style.display = "block";
            if (type === "DICE_ROULETTE") {
                for (let i = 1; i <= cap; i++) {
                    choiceSelect.innerHTML += `<option value="${i}">${i}</option>`;
                }
            } else if (type === "CLOSEST_NUMBER") {
                for (let i = 1; i <= 1000; i++) {
                    choiceSelect.innerHTML += `<option value="${i}">${i}</option>`;
                }
            }
        }
    }
    if (el("user-room-type")) el("user-room-type").addEventListener("change", updateCreatorChoices);
    if (el("user-room-capacity")) el("user-room-capacity").addEventListener("input", updateCreatorChoices);
    el("btn-user-create-room").addEventListener("click", async () => {
        const type = el("user-room-type").value.trim();
        const feeStr = el("user-room-fee").value.trim();
        const capStr = el("user-room-capacity").value.trim();
        const creatorChoice = (el("user-room-creator-choice")?.value || "").trim();
        const parsedFee = parseFloat(feeStr);
        const cap = parseInt(capStr);
        if (!type || !feeStr || isNaN(parsedFee) || parsedFee <= 0 || !capStr || isNaN(cap) || cap < 2 || !creatorChoice) {
            return showToast("Geçersiz veya boş değer girdiniz", "warning");
        }
        const maxCap = (type === "DICE_ROULETTE") ? 6 : 12;
        if (cap < 2 || cap > maxCap) {
            return showToast(`${type} capacity must be between 2 and ${maxCap}.`, "warning");
        }
        try {
            await fetchAPI("/gamerooms/create", {
                method: "POST",
                body: JSON.stringify({
                    gameType: type,
                    entryFee: parsedFee,
                    capacity: cap,
                    creatorId: currentUser.id,
                    creatorChoice: creatorChoice
                })
            });
            showToast("Room created successfully", "success");
            populateDropdowns();
            refreshUserData();
        } catch (e) { }
    });
    el("btn-cancel-join").addEventListener("click", () => {
        hide("join-room-panel");
        el("join-room-choice").value = "";
    });
    el("btn-confirm-join").addEventListener("click", async () => {
        const roomId = el("join-room-id").value;
        const choice = el("join-room-choice").value;
        if (!choice) return showToast("Please enter a choice", "warning");
        try {
            await fetchAPI(`/gamerooms/${roomId}/join`, {
                method: "PUT",
                body: JSON.stringify({ userId: currentUser.id, choice })
            });
            showToast("Joined room successfully!", "success");
            hide("join-room-panel");
            el("join-room-choice").value = "";
            refreshUserData();
        } catch (e) { }
    });
    el("btn-admin-create-event").addEventListener("click", async () => {
        const desc = el("admin-event-desc").value;
        const optA = el("admin-event-opt-a").value;
        const optB = el("admin-event-opt-b").value;
        if (!desc || !optA || !optB) return showToast("Fill all event fields", "warning");
        try {
            await fetchAPI("/events", { 
                method: "POST", 
                body: JSON.stringify({ description: desc, optionA: optA, optionB: optB }) 
            });
            showToast("Event created", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) { }
    });
    el("btn-admin-start-event").addEventListener("click", async () => {
        const eventId = el("admin-start-event-id").value;
        if (!eventId) return showToast("Select an event", "warning");
        try {
            await fetchAPI(`/events/${eventId}/start`, { method: "PUT" });
            showToast("Event started", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) { }
    });
    el("admin-result-event-id").addEventListener("change", async (e) => {
        const eventId = e.target.value;
        const resultSelect = el("admin-result-val");
        resultSelect.innerHTML = '<option value="">Select Winner</option>';
        if (!eventId) return;
        try {
            const ev = await fetchAPI(`/events/${eventId}`);
            if (ev && ev.optionA && ev.optionB) {
                resultSelect.innerHTML += `<option value="${ev.optionA}">${ev.optionA}</option>`;
                resultSelect.innerHTML += `<option value="${ev.optionB}">${ev.optionB}</option>`;
            }
        } catch (e) { }
    });
    el("btn-admin-register-result").addEventListener("click", async () => {
        const eventId = el("admin-result-event-id").value;
        const result = el("admin-result-val").value;
        if (!eventId || !result) return showToast("Fill all fields", "warning");
        try {
            await fetchAPI("/events/result", { method: "POST", body: JSON.stringify({ eventId, finalResult: result }) });
            showToast("Event resulted and settled", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) { }
    });
    el("btn-admin-check-wallet").addEventListener("click", async () => {
        const userId = el("admin-wallet-user-id").value;
        if (!userId) return showToast("Fill User ID", "warning");
        try {
            const wallet = await fetchAPI(`/wallets/${userId}/details`);
            el("admin-display-active").textContent = parseFloat(wallet.availableBalance).toFixed(2);
            el("admin-display-locked").textContent = parseFloat(wallet.lockedBalance).toFixed(2);
            show("admin-wallet-display");
        } catch (e) { }
    });
    el("btn-admin-override-wallet").addEventListener("click", async () => {
        const userId = el("admin-wallet-user-id").value;
        const newBalance = el("admin-wallet-balance").value;
        if (!userId || !newBalance) return showToast("Fill all fields", "warning");
        try {
            await fetchAPI(`/admins/wallets/${userId}`, { 
                method: "PUT", 
                body: JSON.stringify({ newBalance: parseFloat(newBalance) }) 
            });
            showToast("User balance updated", "success");
        } catch (e) { }
    });
    el("btn-admin-view-history").addEventListener("click", async () => {
        const userId = el("admin-history-user-id").value;
        if (!userId) return showToast("Enter User ID", "warning");
        try {
            const history = await fetchAPI(`/history/${userId}`, {
                headers: { "X-User-Id": currentUser.id }
            });
            const betsList = el("admin-history-bets");
            const gamesList = el("admin-history-games");
            betsList.innerHTML = "";
            gamesList.innerHTML = "";
            if (history.bets.length === 0) betsList.innerHTML = "<li>No bets found</li>";
            for (const bet of history.bets) {
                let outcome = "PENDING";
                if (bet.status === "SETTLED") {
                    const ev = await fetchAPI(`/events/${bet.eventId}`);
                    if (ev && ev.finalResult) {
                        const creatorWon = ev.finalResult === bet.creatorSelection;
                        if (creatorWon) {
                            outcome = (bet.creatorId == userId) ? `WON (+$${bet.stake})` : `LOST (-$${bet.stake})`;
                        } else {
                            outcome = (bet.acceptorId == userId) ? `WON (+$${bet.stake})` : `LOST (-$${bet.stake})`;
                        }
                    } else {
                        outcome = "SETTLED";
                    }
                } else if (bet.status === "CANCELLED") {
                    outcome = "REFUNDED";
                } else if (bet.status === "MATCHED") {
                    outcome = "ONGOING";
                }
                let outcomeStyle = outcome.startsWith("WON") ? "text-success" : (outcome.startsWith("LOST") ? "text-danger" : "");
                betsList.innerHTML += `<li>Bet #${bet.id} | Event ${bet.eventId} | Stake $${bet.stake} | Status: ${bet.status} | <strong class="${outcomeStyle}">${outcome}</strong></li>`;
            }
            if (history.games.length === 0) gamesList.innerHTML = "<li>No games found</li>";
            else {
                for (const g of history.games) {
                    let outcome = "PENDING";
                    if (g.status === "FINISHED") {
                        if (g.winnerId == -1) outcome = "REFUNDED";
                        else if (g.winnerId == userId) outcome = `WON (Pool: $${g.poolAmount})`;
                        else outcome = `LOST (-$${g.entryFee})`;
                    } else if (g.status === "IN_PROGRESS") {
                        outcome = "ONGOING";
                    }
                    let outcomeStyle = outcome.startsWith("WON") ? "text-success" : (outcome.startsWith("LOST") ? "text-danger" : "");
                    gamesList.innerHTML += `<li>Room #${g.roomId} (${g.gameType}) - Status: ${g.status} | <strong class="${outcomeStyle}">${outcome}</strong></li>`;
                }
            }
            show("admin-history-display");
        } catch (e) { }
    });
    el("btn-admin-cancel-room").addEventListener("click", async () => {
        const roomId = el("admin-cancel-room-id").value;
        if (!roomId) return showToast("Select a Room ID", "warning");
        try {
            await fetchAPI(`/gamerooms/${roomId}/cancel`, { method: "PUT" });
            showToast("Room cancelled successfully", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) { }
    });
    renderAuth();
}
document.addEventListener("DOMContentLoaded", init);
