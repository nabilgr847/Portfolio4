// --- Stars & Cursor Animation ---
const canvas = document.getElementById('stars');
const ctx = canvas.getContext('2d');
let stars = [];

function resize() { 
    if(canvas) {
        canvas.width = window.innerWidth; 
        canvas.height = window.innerHeight; 
    }
}
window.addEventListener('resize', resize); 
resize();

for(let i=0; i<150; i++) { 
    stars.push({ x: Math.random()*window.innerWidth, y: Math.random()*window.innerHeight, s: Math.random()*1.5, v: Math.random()*0.3 }); 
}

function draw() {
    if(!ctx) return;
    ctx.clearRect(0,0,canvas.width,canvas.height); 
    ctx.fillStyle='#fff';
    stars.forEach(s => { 
        ctx.beginPath(); 
        ctx.arc(s.x, s.y, s.s, 0, Math.PI*2); 
        ctx.fill(); 
        s.y += s.v; 
        if(s.y > canvas.height) s.y = 0; 
    });
    requestAnimationFrame(draw);
} 
draw();

const cur = document.getElementById('cur'), ring = document.getElementById('ring');
let mx=0, my=0, rx=0, ry=0;

document.addEventListener('mousemove', e => { 
    mx = e.clientX; my = e.clientY; 
    if(cur) {
        cur.style.left = mx+'px'; 
        cur.style.top = my+'px'; 
    }
});

(function animRing(){
    if(ring) {
        rx += (mx - rx) * 0.15; ry += (my - ry) * 0.15;
        ring.style.left = rx+'px'; ring.style.top = ry+'px';
    }
    requestAnimationFrame(animRing);
})();

// --- Backend Connection Logic ---
const SERVER_URL = 'http://127.0.0.1:8080';

async function sendToAuth(params) {
    try {
        const body = new URLSearchParams();
        for (const key in params) { body.append(key, params[key]); }
        const response = await fetch(SERVER_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body.toString()
        });
        return await response.text();
    } catch (error) {
        console.error("Connection Error:", error);
        return null; 
    }
}

// নিশ্চিত করা হচ্ছে যে HTML লোড হওয়ার পর ইভেন্টগুলো সেট হবে
window.onload = () => {
    const loginForm = document.getElementById('loginForm');
    const requestBtn = document.getElementById('requestBtn');
    const forgotBtn = document.getElementById('forgotBtn');

    // লগইন হ্যান্ডলার
    if(loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const title = document.querySelector('.login-title');

            title.innerText = "VERIFYING...";
            const res = await sendToAuth({ action: 'login', email: email, password: password });

            // লগইন সফল হলে nobab.html এ যাবে
            if (res && res.trim() === "SUCCESS") {
                title.innerText = "ACCESS GRANTED";
                title.style.color = "#00d4ff";
                setTimeout(() => { window.location.href = "nobab.html"; }, 10); 
            } else {
                title.innerText = "ACCESS DENIED";
                title.style.color = "#ff2f2f";
                alert(res ? "Invalid Key!" : "Server is Offline! টার্মাক্সে সার্ভার চালু করো।");
            }
        });
    }

    // রিকোয়েস্ট একসেস
    if(requestBtn) {
        requestBtn.onclick = async () => {
            let name = prompt("Enter Name:");
            let email = prompt("Enter Email:");
            let customKey = prompt("Create your Security Key (পছন্দের পাসওয়ার্ড দাও):");

            if (name && email && customKey) {
                const res = await sendToAuth({ 
                    action: 'request', 
                    name: name, 
                    email: email, 
                    password: customKey 
                });
                if (res && res.trim() === "REQUEST_RECEIVED") {
                    alert("✅ Success! Your account is created with your key.");
                } else {
                    alert("🚫 Registration failed or User already exists.");
                }
            }
        };
    }

    // ফরগট কি লজিক (সরাসরি পাসওয়ার্ড দেখানোর জন্য ঠিক করা হয়েছে)
    if(forgotBtn) {
        forgotBtn.onclick = async () => {
            let email = prompt("Enter your registered Email:");
            if (email) {
                const res = await sendToAuth({ action: 'forgot', email: email });
                if (res && res.includes("FOUND_KEY:")) {
                    const password = res.split(": ")[1];
                    alert("🔐 Your Security Key is: " + password);
                } else {
                    alert("❌ Email not found!");
                }
            }
        };
    }
};
