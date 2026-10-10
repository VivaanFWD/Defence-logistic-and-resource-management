// Small progressive-enhancement helpers; core navigation/forms work without JavaScript.
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('form').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation();
      }
      form.classList.add('was-validated');
    });
  });

  const from = document.querySelector('input[name="requiredFrom"]');
  const to = document.querySelector('input[name="requiredTo"]');
  if (from && to) {
    from.addEventListener('change', () => {
      to.min = from.value;
      if (to.value && to.value < from.value) to.value = from.value;
    });
  }
});
