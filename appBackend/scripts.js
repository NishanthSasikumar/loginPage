function qs(selector){return document.querySelector(selector)}


// Signup: check password match before submit
const signupForm = qs('#signupForm');
if(signupForm){
signupForm.addEventListener('submit', function(e){
const p = qs('#password').value || '';
const cp = qs('#confirmPassword').value || '';
const err = qs('#signupError');
err.textContent = '';
if(p !== cp){
e.preventDefault();
err.textContent = 'Passwords do not match.';
return false;
}
if(p.length < 6){
e.preventDefault();
err.textContent = 'Password must be at least 6 characters long.';
return false;
}
})
}


// Login: simple client-side required check
const loginForm = qs('#loginForm');
if(loginForm){
loginForm.addEventListener('submit', function(e){
const email = loginForm.querySelector('input[name="email"]').value.trim();
const pass = loginForm.querySelector('input[name="password"]').value.trim();
const err = qs('#loginError'); err.textContent='';
if(!email || !pass){
e.preventDefault(); err.textContent='Please fill both fields.'; return false;
}
})
}


// User profile form: minimal validation
const userForm = qs('#userForm');
if(userForm){
userForm.addEventListener('submit', function(e){
const name = userForm.querySelector('input[name="name"]').value.trim();
const dob = userForm.querySelector('input[name="dob"]').value;
const phone = userForm.querySelector('input[name="phone"]').value.trim();
const skills = userForm.querySelector('input[name="primarySkills"]').value.trim();
const err = qs('#userFormError'); err.textContent='';
if(!name || !dob || !phone || !skills){
e.preventDefault(); err.textContent='Please complete all required fields.'; return false;
}
})
}