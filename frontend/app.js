const API_BASE = "http://localhost:8080/api";
let currentUser = { id: null, type: null, name: null }; // type: 'USER' or 'ADMIN'
let activeEventsMap = {};
let selectedOddKey = null; // e.g., "1001:Lakers"
let currentSlide = 0;
let carouselTimer = null;

const GAME_META = {
    DICE_ROULETTE: { icon: "🎲", label: "Dice Roulette" },
    JACKPOT: { icon: "💰", label: "Jackpot Havuzu" },
    HIGHEST_ROLL: { icon: "🎯", label: "Highest Roll" },
    CLOSEST_NUMBER: { icon: "🔢", label: "Closest Number" },
    HIGH_CARD: { icon: "🃏", label: "High Card" }
};

function showToast(message, type = "info") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transition = "opacity 0.3s ease";
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

async function fetchAPI(endpoint, options = {}) {
    const headers = { "Content-Type": "application/json", ...options.headers };
    if (currentUser.id) {
        if (currentUser.type === "ADMIN") headers["X-Admin-Id"] = currentUser.id;
        if (currentUser.type === "USER") headers["X-User-Id"] = currentUser.id;
    }
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
        let data;
        const text = await response.text();
        try { data = JSON.parse(text); } catch { data = text; }
        if (!response.ok) {
            if (response.status === 400 || response.status === 403 || response.status === 409 || response.status === 401) {
                showToast(typeof data === "string" ? data : data.message || "Bir hata oluştu", "error");
            } else {
                showToast("Sunucu hatası: " + response.status, "error");
            }
            throw new Error(typeof data === "string" ? data : JSON.stringify(data));
        }
        return data;
    } catch (error) {
        console.error("API Error:", error);
        throw error;
    }
}

const el = (id) => document.getElementById(id);
const hide = (id) => el(id)?.classList.add("hidden");
const show = (id) => el(id)?.classList.remove("hidden");

const sortByDateDesc = (a, b) => {
    if (!a.createdAt) return 1;
    if (!b.createdAt) return -1;
    return new Date(b.createdAt.replace(" ", "T")) - new Date(a.createdAt.replace(" ", "T"));
};

const fDate = (d) => (d ? `${d}` : "");

function startClock() {
    const clockEl = el("live-clock");
    if (!clockEl) return;
    const tick = () => {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString("tr-TR", { hour12: false });
    };
    tick();
    setInterval(tick, 1000);
}

function initCarousel() {
    const slides = document.querySelectorAll(".promo-slide");
    const dots = document.querySelectorAll(".carousel-dots .dot");
    if (!slides.length) return;

    const goToSlide = (idx) => {
        currentSlide = (idx + slides.length) % slides.length;
        slides.forEach((s, i) => s.classList.toggle("active", i === currentSlide));
        dots.forEach((d, i) => d.classList.toggle("active", i === currentSlide));
    };

    el("carousel-prev")?.addEventListener("click", () => goToSlide(currentSlide - 1));
    el("carousel-next")?.addEventListener("click", () => goToSlide(currentSlide + 1));
    dots.forEach((dot) => {
        dot.addEventListener("click", () => goToSlide(parseInt(dot.dataset.slide, 10)));
    });

    if (carouselTimer) clearInterval(carouselTimer);
    carouselTimer = setInterval(() => goToSlide(currentSlide + 1), 6000);
}

function init() {
    startClock();
    initCarousel();
    const savedId = localStorage.getItem("p2p_id");
    const savedType = localStorage.getItem("p2p_type");
    const savedName = localStorage.getItem("p2p_name");
    setupEventListeners();
    if (savedId && savedType) {
        currentUser = { id: savedId, type: savedType, name: savedName };
        renderDashboard();
    } else {
        renderAuth();
    }
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
    const displayName = currentUser.name || "Oyuncu";
    el("display-username").textContent = displayName + (currentUser.type === "ADMIN" ? " (Admin)" : "");
    if (el("user-avatar-letter")) {
        el("user-avatar-letter").textContent = displayName.charAt(0).toUpperCase();
    }
    populateDropdowns();
    if (currentUser.type === "USER") {
        show("user-dashboard");
        hide("admin-dashboard");
        show("nav-wallet");
        show("user-nav-links");
        refreshUserData();
    } else {
        show("admin-dashboard");
        hide("user-dashboard");
        hide("nav-wallet");
        hide("user-nav-links");
        fetchAPI("/events/available").then(renderEvents).catch((e) => console.log(e));
        refreshAdminAudit();
    }
}

function logout() {
    localStorage.clear();
    currentUser = { id: null, type: null, name: null };
    renderAuth();
    showToast("Başarıyla çıkış yapıldı", "success");
}

async function populateDropdowns() {
    try {
        if (currentUser.type === "USER") {
            const availableEvents = await fetchAPI("/events/available");
            activeEventsMap = {};
            const betEventSelect = el("bet-event-id");
            if (betEventSelect) {
                const prevVal = betEventSelect.value;
                betEventSelect.innerHTML = '<option value="">Etkinlik Seçin</option>';
                availableEvents.forEach((ev) => {
                    activeEventsMap[ev.id] = ev;
                    betEventSelect.innerHTML += `<option value="${ev.id}">#${ev.id} - ${ev.description}</option>`;
                });
                if (prevVal && activeEventsMap[prevVal]) {
                    betEventSelect.value = prevVal;
                }
            }
        } else if (currentUser.type === "ADMIN") {
            const availableEvents = await fetchAPI("/events/available");
            const allEvents = await fetchAPI("/events");
            const allUsers = await fetchAPI("/admins/users");
            const availableRooms = await fetchAPI("/gamerooms/available");

            const startEventSelect = el("admin-start-event-id");
            if (startEventSelect) {
                startEventSelect.innerHTML = '<option value="">Etkinlik Seçin</option>';
                availableEvents.forEach((ev) => (startEventSelect.innerHTML += `<option value="${ev.id}">${ev.id} - ${ev.description}</option>`));
            }
            const resultEventSelect = el("admin-result-event-id");
            if (resultEventSelect) {
                resultEventSelect.innerHTML = '<option value="">Etkinlik Seçin</option>';
                const inProgressEvents = allEvents.filter((e) => e.status === "IN_PROGRESS");
                inProgressEvents.forEach((ev) => (resultEventSelect.innerHTML += `<option value="${ev.id}">${ev.id} - ${ev.description}</option>`));
            }
            const walletUserSelect = el("admin-wallet-user-id");
            if (walletUserSelect) {
                walletUserSelect.innerHTML = '<option value="">Kullanıcı Seçin</option>';
                allUsers.forEach((u) => (walletUserSelect.innerHTML += `<option value="${u.id}">${u.username} (ID: ${u.id})</option>`));
            }
            const historyUserSelect = el("admin-history-user-id");
            if (historyUserSelect) {
                historyUserSelect.innerHTML = '<option value="">Kullanıcı Seçin</option>';
                allUsers.forEach((u) => (historyUserSelect.innerHTML += `<option value="${u.id}">${u.username} (ID: ${u.id})</option>`));
            }
            const cancelRoomSelect = el("admin-cancel-room-id");
            if (cancelRoomSelect) {
                cancelRoomSelect.innerHTML = '<option value="">Oda Seçin</option>';
                const openRooms = availableRooms.filter((r) => r.status === "OPEN");
                openRooms.forEach((r) => (cancelRoomSelect.innerHTML += `<option value="${r.roomId}">Oda #${r.roomId} (${r.gameType})</option>`));
            }
        }
    } catch (e) {
        console.error("Error populating dropdowns", e);
    }
}

async function refreshAdminAudit() {
    if (currentUser.type !== "ADMIN") return;
    try {
        const events = await fetchAPI("/events");
        const bets = await fetchAPI("/admins/bets");
        const rooms = await fetchAPI("/gamerooms");
        events.sort(sortByDateDesc);
        bets.sort(sortByDateDesc);
        rooms.sort(sortByDateDesc);

        const openRoomsCount = rooms.filter((r) => r.status === "OPEN" || r.status === "IN_PROGRESS").length;
        if (el("kpi-events")) el("kpi-events").textContent = events.length;
        if (el("kpi-bets")) el("kpi-bets").textContent = bets.length;
        if (el("kpi-rooms")) el("kpi-rooms").textContent = openRoomsCount;

        const eventsList = el("admin-all-events-list");
        if (eventsList) {
            eventsList.innerHTML = "";
            if (events.length === 0) eventsList.innerHTML = "<li>Etkinlik bulunamadı</li>";
            events.forEach((ev) => {
                let winnerStr = ev.status === "FINISHED" && ev.finalResult ? ` | <strong class="text-success">Kazanan: ${ev.finalResult}</strong>` : "";
                eventsList.innerHTML += `<li>${ev.createdAt ? `[${ev.createdAt}] ` : ""}#${ev.id} | ${ev.status} | ${ev.description}${winnerStr}</li>`;
            });
        }

        const betsList = el("admin-all-bets-list");
        if (betsList) {
            betsList.innerHTML = "";
            if (bets.length === 0) betsList.innerHTML = "<li>Bahis bulunamadı</li>";
            bets.forEach((b) => {
                let winnerStr = "";
                if (b.status === "SETTLED") {
                    const ev = events.find((e) => e.id === b.eventId);
                    if (ev && ev.finalResult) {
                        const creatorWon = ev.finalResult === b.creatorSelection;
                        winnerStr = creatorWon
                            ? ` | <strong class="text-success">Kazanan: Kurucu (ID: ${b.creatorId})</strong>`
                            : ` | <strong class="text-success">Kazanan: Kabul Eden (ID: ${b.acceptorId})</strong>`;
                    }
                } else if (b.status === "CANCELLED") {
                    winnerStr = ` | <strong>İADE EDİLDİ</strong>`;
                }
                betsList.innerHTML += `<li>${b.createdAt ? `[${b.createdAt}] ` : ""}Bahis #${b.id} | Maç: ${b.eventId} | ${b.status} | $${b.stake} | Seçim: ${b.creatorSelection}${winnerStr}</li>`;
            });
        }

        const roomsList = el("admin-all-rooms-list");
        if (roomsList) {
            roomsList.innerHTML = "";
            if (rooms.length === 0) roomsList.innerHTML = "<li>Oda bulunamadı</li>";
            rooms.forEach((r) => {
                let winnerStr = "";
                if (r.status === "FINISHED") {
                    if (r.winnerId === -1) winnerStr = ` | <strong>İADE EDİLDİ</strong>`;
                    else winnerStr = ` | <strong class="text-success">Kazanan ID: ${r.winnerId}</strong>`;
                }
                roomsList.innerHTML += `<li>${r.createdAt ? `[${r.createdAt}] ` : ""}Oda #${r.roomId} (${r.gameType}) | ${r.status} | Havuz: $${r.poolAmount}${winnerStr}</li>`;
            });
        }

        const activeRoomsList = el("admin-active-rooms-list");
        if (activeRoomsList) {
            activeRoomsList.innerHTML = "";
            const activeRooms = rooms.filter((r) => r.status === "OPEN" || r.status === "IN_PROGRESS");
            if (activeRooms.length === 0) activeRoomsList.innerHTML = "<li>Aktif oda bulunmuyor</li>";
            activeRooms.forEach((r) => {
                activeRoomsList.innerHTML += `<li>Oda #${r.roomId} (${r.gameType}) | ${r.status} | Oyuncu: ${r.participants.length}/${r.capacity}</li>`;
            });
        }
    } catch (e) {
        console.error("Error refreshing admin audit", e);
    }
}

async function refreshUserData() {
    try {
        const wallet = await fetchAPI(`/wallets/${currentUser.id}/details`);
        if (el("nav-wallet-balance")) el("nav-wallet-balance").textContent = parseFloat(wallet.availableBalance).toFixed(2);
        if (el("nav-wallet-locked")) el("nav-wallet-locked").textContent = parseFloat(wallet.lockedBalance).toFixed(2);

        const [bets, rooms, events, allEvents, history] = await Promise.all([
            fetchAPI("/bets/available"),
            fetchAPI("/gamerooms/available"),
            fetchAPI("/events/available"),
            fetchAPI("/events"),
            fetchAPI(`/history/${currentUser.id}`)
        ]);

        allEvents.forEach((ev) => (activeEventsMap[ev.id] = ev));

        renderEvents(events);
        renderBets(bets);
        renderRooms(rooms);
        renderHistory(history);
        renderPendingBets(history.bets);
    } catch (e) {
        console.error("Refresh error", e);
    }
}

function updateSlipPotentialWin() {
    const stake = parseFloat(el("bet-stake")?.value || "0");
    const pot = !isNaN(stake) && stake > 0 ? stake * 2 : 0;
    if (el("slip-potential-win")) {
        el("slip-potential-win").textContent = `$${pot.toFixed(2)}`;
    }
}

window.selectMatchOdd = function (eventId, choice) {
    selectedOddKey = `${eventId}:${choice}`;
    const evSelect = el("bet-event-id");
    const choiceSelect = el("bet-creator-selection");
    if (evSelect) {
        evSelect.value = String(eventId);
        evSelect.dispatchEvent(new Event("change"));
    }
    if (choiceSelect) {
        choiceSelect.value = choice;
    }

    document.querySelectorAll(".odd-btn").forEach((btn) => {
        btn.classList.toggle("selected", btn.dataset.oddKey === selectedOddKey);
    });

    // Switch slip tab to create
    document.querySelectorAll(".slip-tab-btn").forEach((b) => b.classList.toggle("active", b.dataset.slip === "create"));
    show("slip-pane-create");
    hide("slip-pane-active");

    el("bet-stake")?.focus();
    showToast(`${choice} kupona eklendi`, "info");
};

function renderPendingBets(bets) {
    const container = el("my-pending-bets-list");
    if (!container) return;
    container.innerHTML = "";
    let pendingBets = bets.filter((b) => b.status === "PENDING" || b.status === "MATCHED");
    pendingBets.sort(sortByDateDesc);

    if (el("pending-bets-count")) el("pending-bets-count").textContent = pendingBets.length;

    if (pendingBets.length === 0) {
        container.innerHTML = "<p class='item-sub'>Aktif kuponunuz bulunmuyor.</p>";
        return;
    }
    pendingBets.forEach((bet) => {
        const ev = activeEventsMap[bet.eventId];
        const opponentOption = ev ? (bet.creatorSelection === ev.optionA ? ev.optionB : ev.optionA) : "Bilinmiyor";
        const isCreator = bet.creatorId == currentUser.id;
        const myPick = isCreator ? bet.creatorSelection : opponentOption;
        const theirPick = isCreator ? opponentOption : bet.creatorSelection;
        const statusLabel = bet.status === "PENDING" ? "EŞLEŞME BEKLİYOR" : "EŞLEŞTİ (CANLI)";
        const div = document.createElement("div");
        div.className = "list-item";
        div.innerHTML = `
            <div>
                <div class="item-title">Kupon #${bet.id} <span class="text-gold">[${statusLabel}]</span></div>
                <div class="item-sub">${ev ? ev.description : "Maç #" + bet.eventId}</div>
                <div class="item-sub">Tutar: <strong>$${bet.stake}</strong> • Seçim: <strong>${myPick}</strong> (vs ${theirPick})</div>
            </div>
            ${bet.status === "PENDING" ? `<button class="btn btn-danger btn-sm" onclick="cancelBet(${bet.id})">İptal</button>` : ""}
        `;
        container.appendChild(div);
    });
}

window.cancelBet = async function (betId) {
    try {
        await fetchAPI(`/bets/${betId}/cancel?requesterId=${currentUser.id}`, { method: "PUT" });
        showToast("Bahis iptal edildi, bakiye iade edildi.", "success");
        refreshUserData();
    } catch (e) {}
};

function renderBets(bets) {
    const container = el("available-bets-list");
    if (!container) return;
    container.innerHTML = "";
    bets.sort(sortByDateDesc);

    const visibleBets = bets.filter((bet) => bet.creatorId != currentUser.id && activeEventsMap[bet.eventId]);
    if (el("count-bets")) el("count-bets").textContent = visibleBets.length;

    if (visibleBets.length === 0) {
        container.innerHTML = "<p class='item-sub'>Şu anda karşılanabilecek açık 1v1 bahis teklifi yok.</p>";
        return;
    }

    visibleBets.forEach((bet) => {
        const ev = activeEventsMap[bet.eventId];
        const acceptorOption = bet.creatorSelection === ev.optionA ? ev.optionB : ev.optionA;
        const potentialPayout = (parseFloat(bet.stake) * 2).toFixed(2);
        const div = document.createElement("div");
        div.className = "bet-offer-card";
        div.innerHTML = `
            <div class="bet-offer-top">
                <span>#${bet.id} • ${fDate(bet.createdAt)}</span>
                <span class="bet-stake-badge">$${bet.stake} → $${potentialPayout}</span>
            </div>
            <div class="bet-offer-match">${ev.description}</div>
            <div class="bet-offer-picks">
                <span>Rakip: <strong>${bet.creatorSelection}</strong></span>
                <span>Senin Tarafın: <strong class="text-success">${acceptorOption}</strong></span>
            </div>
            <button class="btn btn-deposit btn-sm full-width" onclick="acceptBet(${bet.id})">
                KABUL ET (${acceptorOption})
            </button>
        `;
        container.appendChild(div);
    });
}

function renderRooms(rooms) {
    const container = el("available-rooms-list");
    if (!container) return;
    container.innerHTML = "";
    rooms.sort(sortByDateDesc);

    if (el("count-rooms")) el("count-rooms").textContent = rooms.length;

    if (rooms.length === 0) {
        container.innerHTML = "<p class='item-sub'>Şu anda açık oyun odası bulunmuyor. Yeni bir masa açabilirsiniz.</p>";
        return;
    }

    rooms.forEach((room) => {
        const isInRoom = room.participants.some((p) => p.userId == currentUser.id);
        const meta = GAME_META[room.gameType] || { icon: "🎮", label: room.gameType };
        const pct = Math.min(100, Math.round((room.participants.length / room.capacity) * 100));
        const maxPool = (parseFloat(room.entryFee) * room.capacity).toFixed(2);

        const card = document.createElement("div");
        card.className = "room-casino-card";
        card.innerHTML = `
            <div class="room-card-banner type-${room.gameType}">
                <div class="room-type-title">
                    <span>${meta.icon}</span>
                    <span>${meta.label}</span>
                </div>
                <span class="room-id-tag">ODA #${room.roomId}</span>
            </div>
            <div class="room-card-body">
                <div class="room-stats-row">
                    <div class="room-stat-box">
                        <span>Giriş Ücreti</span>
                        <strong>$${room.entryFee}</strong>
                    </div>
                    <div class="room-stat-box">
                        <span>Hedef Havuz</span>
                        <strong class="text-success">$${maxPool}</strong>
                    </div>
                </div>
                <div class="capacity-bar-wrap">
                    <div class="capacity-label">
                        <span>Oyuncular</span>
                        <span>${room.participants.length} / ${room.capacity}</span>
                    </div>
                    <div class="capacity-track">
                        <div class="capacity-fill" style="width: ${pct}%"></div>
                    </div>
                </div>
                ${
                    isInRoom
                        ? `<button class="btn btn-danger btn-sm full-width" onclick="leaveRoom(${room.roomId})">MASADAN AYRIL (İADE)</button>`
                        : `<button class="btn btn-gold btn-sm full-width" onclick="showJoinPanel(${room.roomId}, '${room.gameType}')">MASAYA OTUR</button>`
                }
            </div>
        `;
        container.appendChild(card);
    });
}

function renderHistory(history) {
    const betsList = el("history-bets");
    const gamesList = el("history-games");
    if (!betsList || !gamesList) return;
    betsList.innerHTML = "";
    gamesList.innerHTML = "";
    history.bets.sort(sortByDateDesc);
    history.games.sort(sortByDateDesc);

    if (history.bets.length === 0) betsList.innerHTML = "<li>Henüz bahis geçmişiniz yok.</li>";
    history.bets.forEach((bet) => {
        let outcome = "BEKLEMEDE";
        if (bet.status === "SETTLED") {
            const ev = activeEventsMap[bet.eventId];
            if (ev && ev.finalResult) {
                const creatorWon = ev.finalResult === bet.creatorSelection;
                if (creatorWon) {
                    outcome = bet.creatorId == currentUser.id ? `KAZANDI (+$${bet.stake})` : `KAYBETTİ (-$${bet.stake})`;
                } else {
                    outcome = bet.acceptorId == currentUser.id ? `KAZANDI (+$${bet.stake})` : `KAYBETTİ (-$${bet.stake})`;
                }
            } else {
                outcome = "SONUÇLANDI";
            }
        } else if (bet.status === "CANCELLED") {
            outcome = "İADE EDİLDİ";
        } else if (bet.status === "MATCHED") {
            outcome = "CANLI / EŞLEŞTİ";
        }
        let colorClass = outcome.startsWith("KAZANDI") ? "text-success" : outcome.startsWith("KAYBETTİ") ? "text-danger" : "text-gold";
        betsList.innerHTML += `<li>${bet.createdAt ? `[${bet.createdAt}] ` : ""}Kupon #${bet.id} • Maç #${bet.eventId} • $${bet.stake} • <strong class="${colorClass}">${outcome}</strong></li>`;
    });

    if (history.games.length === 0) gamesList.innerHTML = "<li>Henüz oyun odası geçmişiniz yok.</li>";
    history.games.forEach((game) => {
        let outcome = "AÇIK";
        if (game.status === "FINISHED") {
            if (game.winnerId == currentUser.id) {
                const profit = game.poolAmount - game.entryFee;
                outcome = `KAZANDI (+$${profit})`;
            } else if (game.winnerId == -1) {
                outcome = "İADE EDİLDİ";
            } else {
                outcome = `KAYBETTİ (-$${game.entryFee})`;
            }
        } else if (game.status === "IN_PROGRESS") {
            outcome = "DEVAM EDİYOR";
        }
        let colorClass = outcome.startsWith("KAZANDI") ? "text-success" : outcome.startsWith("KAYBETTİ") ? "text-danger" : "text-gold";
        gamesList.innerHTML += `<li>${game.createdAt ? `[${game.createdAt}] ` : ""}Oda #${game.roomId} (${game.gameType}) • <strong class="${colorClass}">${outcome}</strong></li>`;
    });
}

function renderEvents(events) {
    const userContainer = el("user-active-events-list");
    const adminContainer = el("admin-active-events-list");
    if (userContainer) userContainer.innerHTML = "";
    if (adminContainer) adminContainer.innerHTML = "";
    events.sort(sortByDateDesc);

    const activeUserEvents = events.filter((ev) => ev.status !== "FINISHED");
    if (el("count-events")) el("count-events").textContent = activeUserEvents.length;
    if (el("count-all-events")) el("count-all-events").textContent = activeUserEvents.length;

    if (activeUserEvents.length === 0 && userContainer) {
        userContainer.innerHTML = "<p class='item-sub'>Şu anda aktif etkinlik bulunmuyor.</p>";
    }
    if (events.length === 0 && adminContainer) {
        adminContainer.innerHTML = "<p class='item-sub'>Aktif etkinlik yok.</p>";
    }

    events.forEach((ev) => {
        if (userContainer && ev.status !== "FINISHED") {
            const isLive = ev.status === "IN_PROGRESS";
            const statusClass = isLive ? "live" : "upcoming";
            const statusText = isLive ? "CANLI" : "YAKLAŞAN";
            const keyA = `${ev.id}:${ev.optionA}`;
            const keyB = `${ev.id}:${ev.optionB}`;

            userContainer.innerHTML += `
                <div class="match-row">
                    <div>
                        <div class="match-meta">
                            <span class="status-pill ${statusClass}">${statusText}</span>
                            <span>Maç #${ev.id}</span>
                            ${ev.createdAt ? `<span>• ${ev.createdAt}</span>` : ""}
                        </div>
                        <div class="match-teams">${ev.description}</div>
                    </div>
                    <div class="match-odds-group">
                        <button type="button" class="odd-btn ${selectedOddKey === keyA ? "selected" : ""}" data-odd-key="${keyA}" onclick="selectMatchOdd(${ev.id}, '${ev.optionA.replace(/'/g, "\\'")}')">
                            <span class="odd-team">${ev.optionA}</span>
                            <span class="odd-val">2.00</span>
                        </button>
                        <button type="button" class="odd-btn ${selectedOddKey === keyB ? "selected" : ""}" data-odd-key="${keyB}" onclick="selectMatchOdd(${ev.id}, '${ev.optionB.replace(/'/g, "\\'")}')">
                            <span class="odd-team">${ev.optionB}</span>
                            <span class="odd-val">2.00</span>
                        </button>
                    </div>
                </div>
            `;
        }

        if (adminContainer) {
            adminContainer.innerHTML += `
                <div class="list-item">
                    <div>
                        <div class="item-title">#${ev.id}: ${ev.description}</div>
                        <div class="item-sub">${ev.optionA} vs ${ev.optionB} | Durum: ${ev.status}</div>
                    </div>
                </div>
            `;
        }
    });
}

window.acceptBet = async function (betId) {
    try {
        await fetchAPI("/bets/match", {
            method: "PUT",
            body: JSON.stringify({ betId, acceptorId: currentUser.id })
        });
        showToast("Bahis başarıyla karşılandı!", "success");
        refreshUserData();
    } catch (e) {}
};

window.showJoinPanel = async function (roomId, gameType) {
    el("join-room-id").value = roomId;
    el("join-room-title").textContent = `Oda #${roomId} (${gameType}) — Seçim Yap`;
    const choiceSelect = el("join-room-choice");
    choiceSelect.innerHTML = '<option value="">-- Seçenekler Yükleniyor --</option>';
    show("join-room-panel");
    el("join-room-panel")?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    try {
        const choices = await fetchAPI(`/gamerooms/${roomId}/choices`);
        if (gameType === "HIGH_CARD" || gameType === "HIGHEST_ROLL" || gameType === "JACKPOT") {
            choiceSelect.style.display = "none";
            choiceSelect.innerHTML = '<option value="AUTO">Otomatik Atama</option>';
        } else {
            choiceSelect.style.display = "block";
            choiceSelect.innerHTML = '<option value="">-- Seçiminizi Yapın --</option>';
            choices.forEach((c) => {
                choiceSelect.innerHTML += `<option value="${c}">${c}</option>`;
            });
            if (choices.length === 0) {
                choiceSelect.innerHTML = '<option value="">-- Oda Dolu --</option>';
            }
        }
    } catch (e) {
        choiceSelect.innerHTML = '<option value="">-- Hata --</option>';
    }
};

window.leaveRoom = async function (roomId) {
    try {
        await fetchAPI(`/gamerooms/${roomId}/leave?userId=${currentUser.id}`, { method: "PUT" });
        showToast("Odadan ayrıldınız. Giriş ücreti iade edildi.", "success");
        refreshUserData();
    } catch (e) {}
};

function filterDashboardSection(target) {
    const sections = ["events", "bets", "rooms", "history"];
    sections.forEach((sec) => {
        const node = el(`section-${sec}`);
        if (!node) return;
        if (target === "all" || target === sec) {
            node.classList.remove("hidden");
        } else {
            node.classList.add("hidden");
        }
    });
    document.querySelectorAll(".cat-link").forEach((b) => b.classList.toggle("active", b.dataset.section === target));
    document.querySelectorAll("#user-nav-links .nav-item").forEach((a) => a.classList.toggle("active", a.dataset.filter === target));
}

function setupEventListeners() {
    // Auth Tabs
    document.querySelectorAll(".tab-btn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
            document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
            document.querySelectorAll(".auth-content").forEach((c) => c.classList.add("hidden"));
            e.target.classList.add("active");
            show(e.target.dataset.target);
        });
    });

    // Category Sidebar & Top Nav Filters
    document.querySelectorAll(".cat-link").forEach((btn) => {
        btn.addEventListener("click", () => filterDashboardSection(btn.dataset.section));
    });
    document.querySelectorAll("#user-nav-links .nav-item").forEach((link) => {
        link.addEventListener("click", (e) => {
            e.preventDefault();
            filterDashboardSection(link.dataset.filter || "all");
        });
    });
    el("logo-home")?.addEventListener("click", (e) => {
        e.preventDefault();
        if (currentUser.type === "USER") filterDashboardSection("all");
    });

    // Quick Games Sidebar -> Open Create Room Panel
    document.querySelectorAll(".quick-game-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            filterDashboardSection("all");
            show("create-room-panel");
            if (el("user-room-type")) {
                el("user-room-type").value = btn.dataset.gametype;
                el("user-room-type").dispatchEvent(new Event("change"));
            }
            el("section-rooms")?.scrollIntoView({ behavior: "smooth" });
        });
    });

    // Create Room Toggle
    el("btn-toggle-create-room")?.addEventListener("click", () => {
        el("create-room-panel")?.classList.toggle("hidden");
    });
    el("btn-close-create-room")?.addEventListener("click", () => hide("create-room-panel"));

    // Bet Slip Tabs
    document.querySelectorAll(".slip-tab-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".slip-tab-btn").forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            if (btn.dataset.slip === "create") {
                show("slip-pane-create");
                hide("slip-pane-active");
            } else {
                hide("slip-pane-create");
                show("slip-pane-active");
            }
        });
    });

    // Quick Stakes & Potential Win
    el("bet-stake")?.addEventListener("input", updateSlipPotentialWin);
    document.querySelectorAll(".quick-stake-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            if (el("bet-stake")) {
                el("bet-stake").value = btn.dataset.stake;
                updateSlipPotentialWin();
            }
        });
    });

    // Wallet Modal Quick Amounts
    document.querySelectorAll(".quick-amt-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            if (el("wallet-modal-amount")) el("wallet-modal-amount").value = btn.dataset.amt;
        });
    });

    el("logout-btn").addEventListener("click", logout);

    // User Register / Login
    el("btn-user-register").addEventListener("click", async () => {
        const username = el("user-username").value;
        const password = el("user-password").value;
        if (!username || !password) return showToast("Tüm alanları doldurun", "warning");
        try {
            await fetchAPI("/users/register", { method: "POST", body: JSON.stringify({ username, password }) });
            showToast("Kayıt başarılı! Şimdi giriş yapabilirsiniz.", "success");
        } catch (e) {}
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
            showToast("Giriş başarılı", "success");
            renderDashboard();
        } catch (e) {}
    });

    // Forgot & Reset Password
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
        if (!email) return showToast("E-posta adresinizi girin", "warning");
        try {
            const result = await fetchAPI("/users/forgot-password", {
                method: "POST",
                body: JSON.stringify({ email })
            });
            showToast(result, "success");
            hide("forgot-password-auth");
            show("reset-password-auth");
        } catch (e) {}
    });

    el("btn-cancel-reset").addEventListener("click", () => {
        hide("reset-password-auth");
        show("user-auth");
    });
    el("reset-password-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const token = el("reset-token").value;
        const newPassword = el("reset-new-password").value;
        if (!token || !newPassword) return showToast("Tüm alanları doldurun", "warning");
        try {
            const result = await fetchAPI("/users/reset-password", {
                method: "POST",
                body: JSON.stringify({ token, newPassword })
            });
            showToast(result, "success");
            hide("reset-password-auth");
            show("user-auth");
            el("user-password").value = "";
        } catch (e) {}
    });

    // Admin Register / Login
    el("btn-admin-register").addEventListener("click", async () => {
        const username = el("admin-username").value;
        const password = el("admin-password").value;
        const secret = el("admin-secret").value;
        if (!username || !password) return showToast("Tüm alanları doldurun", "warning");
        try {
            await fetchAPI("/admins/register", {
                method: "POST",
                headers: { "X-Admin-Secret": secret || "SUPER_SECRET_KEY" },
                body: JSON.stringify({ username, password })
            });
            showToast("Admin kaydı başarılı.", "success");
        } catch (e) {}
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
            showToast("Yönetici girişi başarılı", "success");
            renderDashboard();
        } catch (e) {}
    });

    // Wallet Modal Controls
    const openWalletModal = (action) => {
        const isDep = action === "deposit";
        el("wallet-modal-title").textContent = isDep ? "Para Yatır" : "Para Çek";
        el("wallet-modal-subtitle").textContent = isDep
            ? "Cüzdanınıza anında bakiye yüklemek için tutar seçin."
            : "Aktif bakiyenizden çekmek istediğiniz tutarı girin.";
        el("wallet-modal-action").value = action;
        el("wallet-modal-amount").value = "";
        show("wallet-modal");
    };

    el("nav-btn-deposit").addEventListener("click", () => openWalletModal("deposit"));
    el("promo-deposit-btn")?.addEventListener("click", () => openWalletModal("deposit"));
    el("nav-btn-withdraw").addEventListener("click", () => openWalletModal("withdraw"));
    el("btn-modal-cancel").addEventListener("click", () => hide("wallet-modal"));
    el("btn-modal-close-x")?.addEventListener("click", () => hide("wallet-modal"));

    el("btn-modal-confirm").addEventListener("click", async () => {
        const action = el("wallet-modal-action").value;
        const amountStr = el("wallet-modal-amount").value.trim();
        const amount = parseFloat(amountStr);
        if (!amountStr || isNaN(amount) || amount <= 0) {
            return showToast("Geçersiz veya boş değer girdiniz", "warning");
        }
        try {
            await fetchAPI(`/wallets/${action}`, { method: "POST", body: JSON.stringify({ userId: currentUser.id, amount }) });
            showToast(`${action === "deposit" ? "Para yatırma" : "Para çekme"} işlemi başarılı`, "success");
            hide("wallet-modal");
            refreshUserData();
        } catch (e) {}
    });

    // Bet Creation
    el("bet-event-id").addEventListener("change", (e) => {
        const ev = activeEventsMap[e.target.value];
        const selectionSelect = el("bet-creator-selection");
        selectionSelect.innerHTML = '<option value="">-- Taraf Seçin --</option>';
        if (ev && ev.optionA && ev.optionB) {
            selectionSelect.innerHTML += `<option value="${ev.optionA}">${ev.optionA}</option>`;
            selectionSelect.innerHTML += `<option value="${ev.optionB}">${ev.optionB}</option>`;
        }
    });

    el("btn-create-bet").addEventListener("click", async () => {
        const eventIdStr = el("bet-event-id").value.trim();
        const stakeStr = el("bet-stake").value.trim();
        const creatorSelection = el("bet-creator-selection").value.trim();
        const eventId = parseInt(eventIdStr, 10);
        const stake = parseFloat(stakeStr);
        if (!eventIdStr || isNaN(eventId) || eventId <= 0 || !stakeStr || isNaN(stake) || stake <= 0 || !creatorSelection) {
            return showToast("Geçersiz veya boş değer girdiniz", "warning");
        }
        try {
            await fetchAPI("/bets", {
                method: "POST",
                body: JSON.stringify({ creatorId: currentUser.id, eventId, stake, creatorSelection })
            });
            showToast("Bahis teklifi oluşturuldu!", "success");
            el("bet-stake").value = "";
            el("bet-creator-selection").value = "";
            selectedOddKey = null;
            updateSlipPotentialWin();
            refreshUserData();
        } catch (e) {}
    });

    // Room Creation
    function updateCreatorChoices() {
        const type = el("user-room-type").value;
        const capStr = el("user-room-capacity").value;
        const choiceSelect = el("user-room-creator-choice");
        const wrap = el("creator-choice-wrap");
        if (!choiceSelect) return;
        choiceSelect.innerHTML = '<option value="">-- Başlangıç Seçimi --</option>';
        if (!type) return;

        if (type === "HIGH_CARD" || type === "HIGHEST_ROLL" || type === "JACKPOT") {
            if (wrap) wrap.style.display = "none";
            choiceSelect.innerHTML = '<option value="AUTO" selected>Otomatik Atama</option>';
            return;
        }

        if (wrap) wrap.style.display = "block";
        if (!capStr) return;
        let cap = parseInt(capStr, 10);
        if (isNaN(cap) || cap < 2) return;
        if (type === "DICE_ROULETTE") {
            cap = Math.min(cap, 6);
            for (let i = 1; i <= cap; i++) {
                choiceSelect.innerHTML += `<option value="${i}">${i}</option>`;
            }
        } else if (type === "CLOSEST_NUMBER") {
            for (let i = 1; i <= 1000; i++) {
                choiceSelect.innerHTML += `<option value="${i}">${i}</option>`;
            }
        }
    }

    el("user-room-type")?.addEventListener("change", updateCreatorChoices);
    el("user-room-capacity")?.addEventListener("input", updateCreatorChoices);

    el("btn-user-create-room").addEventListener("click", async () => {
        const type = el("user-room-type").value.trim();
        const feeStr = el("user-room-fee").value.trim();
        const capStr = el("user-room-capacity").value.trim();
        const creatorChoice = (el("user-room-creator-choice")?.value || "").trim();
        const parsedFee = parseFloat(feeStr);
        const cap = parseInt(capStr, 10);
        if (!type || !feeStr || isNaN(parsedFee) || parsedFee <= 0 || !capStr || isNaN(cap) || cap < 2 || !creatorChoice) {
            return showToast("Geçersiz veya boş değer girdiniz", "warning");
        }
        const maxCap = type === "DICE_ROULETTE" ? 6 : 12;
        if (cap < 2 || cap > maxCap) {
            return showToast(`${type} kapasitesi 2 ile ${maxCap} arasında olmalıdır.`, "warning");
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
            showToast("Oyun odası oluşturuldu!", "success");
            hide("create-room-panel");
            populateDropdowns();
            refreshUserData();
        } catch (e) {}
    });

    el("btn-cancel-join").addEventListener("click", () => {
        hide("join-room-panel");
        el("join-room-choice").value = "";
    });

    el("btn-confirm-join").addEventListener("click", async () => {
        const roomId = el("join-room-id").value;
        const choice = el("join-room-choice").value;
        if (!choice) return showToast("Lütfen bir seçim yapın", "warning");
        try {
            await fetchAPI(`/gamerooms/${roomId}/join`, {
                method: "PUT",
                body: JSON.stringify({ userId: currentUser.id, choice })
            });
            showToast("Odaya katıldınız!", "success");
            hide("join-room-panel");
            el("join-room-choice").value = "";
            refreshUserData();
        } catch (e) {}
    });

    // Admin Controls
    el("btn-admin-create-event").addEventListener("click", async () => {
        const desc = el("admin-event-desc").value;
        const optA = el("admin-event-opt-a").value;
        const optB = el("admin-event-opt-b").value;
        if (!desc || !optA || !optB) return showToast("Tüm etkinlik alanlarını doldurun", "warning");
        try {
            await fetchAPI("/events", {
                method: "POST",
                body: JSON.stringify({ description: desc, optionA: optA, optionB: optB })
            });
            showToast("Etkinlik oluşturuldu", "success");
            el("admin-event-desc").value = "";
            el("admin-event-opt-a").value = "";
            el("admin-event-opt-b").value = "";
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) {}
    });

    el("btn-admin-start-event").addEventListener("click", async () => {
        const eventId = el("admin-start-event-id").value;
        if (!eventId) return showToast("Bir etkinlik seçin", "warning");
        try {
            await fetchAPI(`/events/${eventId}/start`, { method: "PUT" });
            showToast("Etkinlik başlatıldı", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) {}
    });

    el("admin-result-event-id").addEventListener("change", async (e) => {
        const eventId = e.target.value;
        const resultSelect = el("admin-result-val");
        resultSelect.innerHTML = '<option value="">Kazananı Seçin</option>';
        if (!eventId) return;
        try {
            const ev = await fetchAPI(`/events/${eventId}`);
            if (ev && ev.optionA && ev.optionB) {
                resultSelect.innerHTML += `<option value="${ev.optionA}">${ev.optionA}</option>`;
                resultSelect.innerHTML += `<option value="${ev.optionB}">${ev.optionB}</option>`;
            }
        } catch (e) {}
    });

    el("btn-admin-register-result").addEventListener("click", async () => {
        const eventId = el("admin-result-event-id").value;
        const result = el("admin-result-val").value;
        if (!eventId || !result) return showToast("Tüm alanları doldurun", "warning");
        try {
            await fetchAPI("/events/result", { method: "POST", body: JSON.stringify({ eventId, finalResult: result }) });
            showToast("Etkinlik sonuçlandırıldı ve ödemeler dağıtıldı", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) {}
    });

    el("btn-admin-check-wallet").addEventListener("click", async () => {
        const userId = el("admin-wallet-user-id").value;
        if (!userId) return showToast("Kullanıcı seçin", "warning");
        try {
            const wallet = await fetchAPI(`/wallets/${userId}/details`);
            el("admin-display-active").textContent = parseFloat(wallet.availableBalance).toFixed(2);
            el("admin-display-locked").textContent = parseFloat(wallet.lockedBalance).toFixed(2);
            show("admin-wallet-display");
        } catch (e) {}
    });

    el("btn-admin-override-wallet").addEventListener("click", async () => {
        const userId = el("admin-wallet-user-id").value;
        const newBalance = el("admin-wallet-balance").value;
        if (!userId || !newBalance) return showToast("Tüm alanları doldurun", "warning");
        try {
            await fetchAPI(`/admins/wallets/${userId}`, {
                method: "PUT",
                body: JSON.stringify({ newBalance: parseFloat(newBalance) })
            });
            showToast("Kullanıcı bakiyesi güncellendi", "success");
        } catch (e) {}
    });

    el("btn-admin-view-history").addEventListener("click", async () => {
        const userId = el("admin-history-user-id").value;
        if (!userId) return showToast("Kullanıcı seçin", "warning");
        try {
            const history = await fetchAPI(`/history/${userId}`, {
                headers: { "X-User-Id": currentUser.id }
            });
            const betsList = el("admin-history-bets");
            const gamesList = el("admin-history-games");
            betsList.innerHTML = "";
            gamesList.innerHTML = "";
            if (history.bets.length === 0) betsList.innerHTML = "<li>Bahis bulunamadı</li>";
            for (const bet of history.bets) {
                let outcome = "BEKLEMEDE";
                if (bet.status === "SETTLED") {
                    const ev = await fetchAPI(`/events/${bet.eventId}`);
                    if (ev && ev.finalResult) {
                        const creatorWon = ev.finalResult === bet.creatorSelection;
                        outcome = creatorWon
                            ? bet.creatorId == userId ? `KAZANDI (+$${bet.stake})` : `KAYBETTİ (-$${bet.stake})`
                            : bet.acceptorId == userId ? `KAZANDI (+$${bet.stake})` : `KAYBETTİ (-$${bet.stake})`;
                    } else {
                        outcome = "SONUÇLANDI";
                    }
                } else if (bet.status === "CANCELLED") {
                    outcome = "İADE EDİLDİ";
                } else if (bet.status === "MATCHED") {
                    outcome = "CANLI";
                }
                let outcomeStyle = outcome.startsWith("KAZANDI") ? "text-success" : outcome.startsWith("KAYBETTİ") ? "text-danger" : "";
                betsList.innerHTML += `<li>Bahis #${bet.id} | Maç ${bet.eventId} | $${bet.stake} | ${bet.status} | <strong class="${outcomeStyle}">${outcome}</strong></li>`;
            }
            if (history.games.length === 0) gamesList.innerHTML = "<li>Oyun bulunamadı</li>";
            else {
                for (const g of history.games) {
                    let outcome = "BEKLEMEDE";
                    if (g.status === "FINISHED") {
                        if (g.winnerId == -1) outcome = "İADE EDİLDİ";
                        else if (g.winnerId == userId) outcome = `KAZANDI (Havuz: $${g.poolAmount})`;
                        else outcome = `KAYBETTİ (-$${g.entryFee})`;
                    } else if (g.status === "IN_PROGRESS") {
                        outcome = "DEVAM EDİYOR";
                    }
                    let outcomeStyle = outcome.startsWith("KAZANDI") ? "text-success" : outcome.startsWith("KAYBETTİ") ? "text-danger" : "";
                    gamesList.innerHTML += `<li>Oda #${g.roomId} (${g.gameType}) - ${g.status} | <strong class="${outcomeStyle}">${outcome}</strong></li>`;
                }
            }
            show("admin-history-display");
        } catch (e) {}
    });

    el("btn-admin-cancel-room").addEventListener("click", async () => {
        const roomId = el("admin-cancel-room-id").value;
        if (!roomId) return showToast("Bir oda seçin", "warning");
        try {
            await fetchAPI(`/gamerooms/${roomId}/cancel`, { method: "PUT" });
            showToast("Oda iptal edildi ve ücretler iade edildi", "success");
            populateDropdowns();
            refreshAdminAudit();
        } catch (e) {}
    });
}

document.addEventListener("DOMContentLoaded", init);
