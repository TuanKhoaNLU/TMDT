const currentPath = window.location.pathname;

document.querySelectorAll("[data-nav]").forEach((link) => {
  const target = link.getAttribute("href");
  if (!target) return;

  const active =
    target === currentPath ||
    (target !== "/" && currentPath.startsWith(target));

  if (active) {
    link.classList.add("active");
    link.setAttribute("aria-current", "page");
  }
});
