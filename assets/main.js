/* Matthew Schaub ePortfolio — light interactivity, progressive enhancement */
(function () {
  "use strict";

  // Mark JS active so reveal styles apply (content stays visible without JS).
  document.documentElement.classList.add("js");

  // --- mobile nav toggle ---
  var toggle = document.querySelector(".nav__toggle");
  var nav = document.getElementById("primary-nav");
  if (toggle && nav) {
    toggle.addEventListener("click", function () {
      var open = nav.classList.toggle("is-open");
      toggle.setAttribute("aria-expanded", String(open));
    });
    nav.addEventListener("click", function (e) {
      if (e.target.tagName === "A") {
        nav.classList.remove("is-open");
        toggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  // --- scroll reveal ---
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var revealables = document.querySelectorAll(".reveal");
  if (reduce || !("IntersectionObserver" in window)) {
    revealables.forEach(function (el) { el.classList.add("is-in"); });
  } else {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-in");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -8% 0px" });
    revealables.forEach(function (el) { io.observe(el); });

    // Safety net: never leave content hidden. Reveal anything still hidden
    // shortly after load (covers fast scroll, observer edge cases, capture).
    window.addEventListener("load", function () {
      setTimeout(function () {
        revealables.forEach(function (el) {
          var r = el.getBoundingClientRect();
          if (r.top < window.innerHeight + 80) el.classList.add("is-in");
        });
      }, 250);
    });
    setTimeout(function () {
      revealables.forEach(function (el) { el.classList.add("is-in"); });
    }, 2600);
  }

  // --- active section highlighting  ---
  var sectionLinks = Array.prototype.slice.call(
    document.querySelectorAll('.nav a[href^="#"]')
  );
  if (sectionLinks.length && "IntersectionObserver" in window) {
    var map = {};
    sectionLinks.forEach(function (link) {
      var id = link.getAttribute("href").slice(1);
      var sec = document.getElementById(id);
      if (sec) map[id] = link;
    });
    var spy = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          sectionLinks.forEach(function (l) { l.removeAttribute("aria-current"); });
          var active = map[entry.target.id];
          if (active) active.setAttribute("aria-current", "true");
        }
      });
    }, { rootMargin: "-45% 0px -50% 0px" });
    Object.keys(map).forEach(function (id) {
      var sec = document.getElementById(id);
      if (sec) spy.observe(sec);
    });
  }

  // --- footer year ---
  var y = document.querySelector("[data-year]");
  if (y) y.textContent = new Date().getFullYear();
})();
