/**
 * 交互粒子系统 — 淡紫/海蓝色调
 */
(function(){
    var canvas = document.getElementById('particle-canvas');
    if(!canvas) return;
    var ctx = canvas.getContext('2d');
    var w, h, particles = [];
    var mouse = { x: -200, y: -200 };
    var MAX = 55;

    function resize(){
        var p = canvas.parentElement;
        w = canvas.width = p.offsetWidth;
        h = canvas.height = p.offsetHeight;
    }
    resize();
    window.addEventListener('resize', resize);

    // 鼠标 / 触摸
    canvas.parentElement.addEventListener('mousemove', function(e){ var r = canvas.parentElement.getBoundingClientRect(); mouse.x = e.clientX - r.left; mouse.y = e.clientY - r.top; });
    canvas.parentElement.addEventListener('mouseleave', function(){ mouse.x = -200; mouse.y = -200; });
    canvas.parentElement.addEventListener('touchmove', function(e){ var r = canvas.parentElement.getBoundingClientRect(); mouse.x = e.touches[0].clientX - r.left; mouse.y = e.touches[0].clientY - r.top; }, {passive:true});
    canvas.parentElement.addEventListener('touchend', function(){ mouse.x = -200; mouse.y = -200; });

    function Particle(){
        this.reset();
    }
    Particle.prototype.reset = function(){
        this.x = Math.random() * w;
        this.y = h + 10;
        this.vx = (Math.random() - 0.5) * 0.4;
        this.vy = -(Math.random() * 0.6 + 0.2);
        this.r = Math.random() * 3 + 1.2;
        this.a = Math.random() * 0.5 + 0.15;
        var c = ['199,210,254','186,230,253','221,214,254','196,181,253','255,255,255'][Math.floor(Math.random()*5)];
        this.color = 'rgba(' + c + ',';
    };
    Particle.prototype.update = function(){
        var dx = mouse.x - this.x, dy = mouse.y - this.y;
        var dist = Math.sqrt(dx*dx + dy*dy);
        if(dist < 120){
            var f = (120 - dist) / 120;
            this.vx += dx * f * 0.0008;
            this.vy += dy * f * 0.0008;
        }
        this.vx *= 0.998;
        this.vy *= 0.998;
        this.x += this.vx;
        this.y += this.vy;
        if(this.y < -20 || this.x < -20 || this.x > w + 20) this.reset();
    };
    Particle.prototype.draw = function(){
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.r, 0, Math.PI*2);
        ctx.fillStyle = this.color + this.a + ')';
        ctx.fill();
    };

    for(var i = 0; i < MAX; i++){
        var p = new Particle();
        p.y = Math.random() * h;
        particles.push(p);
    }

    function animate(){
        ctx.clearRect(0, 0, w, h);
        for(var i = 0; i < particles.length; i++){
            particles[i].update();
            particles[i].draw();
        }
        requestAnimationFrame(animate);
    }
    animate();
})();
