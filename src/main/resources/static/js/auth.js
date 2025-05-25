// src/main/resources/static/js/auth.js

document.addEventListener('DOMContentLoaded', function() {
    // --- Get references to elements that MIGHT exist on the current page ---
    // Login Page Elements
    const loginForm = document.getElementById('loginForm');
    const loginMessageDiv = document.getElementById('errorMessage'); // Used on login.html

    // Registration Page Elements
    const registerForm = document.getElementById('registerForm');
    const registerMessageDiv = document.getElementById('registerMessage'); // For register.html form
    const roleSelect = document.getElementById('role');
    const employeeSpecificFieldsDiv = document.getElementById('employeeSpecificFields'); // The main container for employee fields
    const departmentIdSelectInRegister = document.getElementById('departmentId'); // Department select within employeeSpecificFields
    const managedByAdminSelect = document.getElementById('managedByAdminId'); // Admin manager select within employeeSpecificFields

    // OTP Verification Elements (assuming they are on register.html or a separate verify_otp.html)
    const otpForm = document.getElementById('otpForm');
    const otpMessageDiv = document.getElementById('otpMessage');
    const otpEmailDisplay = document.getElementById('otpEmailDisplay');
    const otpVerificationEmailInput = document.getElementById('otpVerificationEmail');
    const resendOtpLink = document.getElementById('resendOtpLink');
    const backToRegisterLink = document.getElementById('backToRegisterLink'); // Link on OTP form to go back

    // --- Conditional Event Listeners based on page/form presence ---
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    if (registerForm) {
        registerForm.addEventListener('submit', handleRegistration);

        if (roleSelect) {
            roleSelect.addEventListener('change', function(event) {
                const selectedRole = event.target.value;
                toggleEmployeeSpecificFields(selectedRole, employeeSpecificFieldsDiv, registerForm);

                // SCENARIO 2 Change: If role becomes Employee, potentially populate admins if not already.
                // Department population is now tied to admin selection.
                if (selectedRole === 'EMPLOYEE') {
                    if (managedByAdminSelect && (managedByAdminSelect.options.length <= 1 || managedByAdminSelect.disabled)) { // <=1 to account for placeholder
                        populateAdminManagersDropdown(managedByAdminSelect);
                    }
                    // Clear and disable department dropdown initially for employee role
                    if (departmentIdSelectInRegister) {
                        departmentIdSelectInRegister.innerHTML = '<option value="" disabled selected>Select manager first</option>';
                        departmentIdSelectInRegister.disabled = true;
                    }
                }
            });
            // Initial call
            toggleEmployeeSpecificFields(roleSelect.value, employeeSpecificFieldsDiv, registerForm);
        }

        // Populate Admin Managers dropdown on page load (it will be shown/hidden by toggle)
        if (managedByAdminSelect) {
            populateAdminManagersDropdown(managedByAdminSelect);

            // SCENARIO 2 Change: Add event listener to the Admin Manager dropdown
            managedByAdminSelect.addEventListener('change', function(event) {
                const selectedAdminId = event.target.value;
                if (selectedAdminId && departmentIdSelectInRegister) {
                    populateDepartmentsForSelectedAdmin(departmentIdSelectInRegister, selectedAdminId);
                } else if (departmentIdSelectInRegister) {
                    // If no admin is selected, clear/disable department dropdown
                    departmentIdSelectInRegister.innerHTML = '<option value="" disabled selected>Select manager first</option>';
                    departmentIdSelectInRegister.disabled = true;
                }
            });
        }
    }

    if (otpForm) { // If OTP form exists on the current page
        otpForm.addEventListener('submit', handleOtpVerification);
        // Logic to pre-fill email on OTP form if coming from register.html or via URL param
        // If OTP form is part of register.html, this part might be handled differently (e.g., when switching views)
        const urlParams = new URLSearchParams(window.location.search);
        const emailFromUrl = urlParams.get('email');
        if (emailFromUrl) {
            if (otpEmailDisplay) otpEmailDisplay.textContent = decodeURIComponent(emailFromUrl);
            if (otpVerificationEmailInput) otpVerificationEmailInput.value = decodeURIComponent(emailFromUrl);
        } else if (document.getElementById('otpVerificationCard') && document.getElementById('otpVerificationCard').style.display !== 'none') {
            // If OTP card is visible on register.html and no URL param, it should have been set by handleRegistration
            // No action needed here if handleRegistration sets otpVerificationEmailInput.value
        }
    }

    if (resendOtpLink) {
        resendOtpLink.addEventListener('click', handleResendOtp);
    }

    if (backToRegisterLink) {
        backToRegisterLink.addEventListener('click', function(e) {
            e.preventDefault();
            const registrationCardEl = document.getElementById('registrationCard');
            const otpCardEl = document.getElementById('otpVerificationCard');
            if (registrationCardEl) registrationCardEl.style.display = 'block';
            if (otpCardEl) otpCardEl.style.display = 'none';
            clearMessages(registerMessageDiv, otpMessageDiv);
        });
    }

    // --- LOGIN FUNCTION ---
    async function handleLogin(event) {
        event.preventDefault();
        const form = event.target;
        const email = form.email.value;
        const password = form.password.value;
        const submitButton = form.querySelector('button[type="submit"]');

        clearMessages(loginMessageDiv); // Use loginMessageDiv
        if(submitButton) submitButton.disabled = true;

        try {
            const data = await makeApiCall('/api/auth/login', 'POST', { email, password });
            storeLoggedInUser(data, data.token || 'dummy_jwt_token');

            if (data.role === 'ADMIN') {
                window.location.href = 'dashboard_admin.html';
            } else if (data.role === 'EMPLOYEE') {
                window.location.href = 'dashboard_employee.html';
            } else {
                window.location.href = 'index.html';
            }
        } catch (error) {
            console.error('Login error:', error);
            if (error.message && error.message.toLowerCase().includes('not verified')) {
                const verifyLink = `verify_otp.html?email=${encodeURIComponent(email)}`;
                displayMessage(loginMessageDiv, `${error.message} <a href="${verifyLink}">Click here to verify or resend OTP.</a>`, 'error', true);
            } else {
                displayMessage(loginMessageDiv, error.message || 'Login failed. Please try again.', 'error');
            }
        } finally {
            if(submitButton) submitButton.disabled = false;
        }
    }

    // --- REGISTRATION FUNCTION ---
    async function handleRegistration(event) {
        event.preventDefault();
        const form = event.target; // This is registerForm
        const submitButton = form.querySelector('button[type="submit"]');

        console.log("Attempting registration...");
        clearMessages(registerMessageDiv); // Use registerMessageDiv
        if(submitButton) submitButton.disabled = true;

        const userEmailForOtp = form.email.value;
        const selectedRole = form.role.value;

        const registrationData = {
            firstName: form.firstName.value,
            lastName: form.lastName.value,
            email: userEmailForOtp,
            password: form.password.value,
            role: selectedRole,
        };

        if (form.gender && form.gender.value) {
             registrationData.gender = form.gender.value; // Assuming DTO supports gender
        }

        if (selectedRole === 'EMPLOYEE') {
            // Check if the employee specific fields div is actually visible
            if (employeeSpecificFieldsDiv && employeeSpecificFieldsDiv.style.display !== 'none') {
                registrationData.dateOfBirth = form.dateOfBirth.value || null;
                registrationData.hireDate = form.hireDate.value || null;
                registrationData.salary = form.salary.value ? parseFloat(form.salary.value) : null;
                registrationData.departmentId = form.departmentId.value ? parseInt(form.departmentId.value) : null;

                const adminSelect = form.managedByAdminId; // Access by name/id from form
                if (adminSelect && adminSelect.value) {
                    registrationData.managedByAdminId = parseInt(adminSelect.value);
                } else if (adminSelect && adminSelect.required) { // Check if it was marked as required
                    displayMessage(registerMessageDiv, "Please select your manager/admin.", "error");
                    if(submitButton) submitButton.disabled = false;
                    return;
                } else {
                    registrationData.managedByAdminId = null; // If optional and not selected
                }

                const deptSelect = form.departmentId;
                                if (deptSelect && deptSelect.value) {
                                    registrationData.departmentId = parseInt(deptSelect.value);
                                } else {
                                    registrationData.departmentId = null;
                                }
            } else {
                // This indicates a potential issue if role is EMPLOYEE but fields are not visible for data collection
                console.warn("Employee role selected, but specific fields div is hidden. Sending minimal data.");
                // Ensure backend handles these as null or defaults them appropriately
                registrationData.managedByAdminId = null;
            }
        }
        console.log("Registration Data being sent:", JSON.stringify(registrationData, null, 2));

        try {
            const responseData = await makeApiCall('/api/auth/register', 'POST', registrationData, {}, true, "Creating account...");
            console.log("Registration API response (responseData):", JSON.stringify(responseData, null, 2));

            console.log("Registration API response:", JSON.stringify(responseData, null, 2));

                        if (responseData && responseData.email) {
                            // ***** FIX: REDIRECT TO verify_otp.html *****
                            console.log("Redirecting to OTP page for email:", responseData.email);
                            window.location.href = `verify_otp.html?email=${encodeURIComponent(responseData.email)}`;
                            // The form.reset() and toggleEmployeeSpecificFields() calls are not needed here
                            // because the page is navigating away.
                        } else {
                            console.error("Registration succeeded but did not receive expected email for OTP step from backend.", responseData);
                            displayMessage(registerMessageDiv, "Account creation initiated, but there was an issue proceeding to OTP verification. Please contact support.", 'error');
                        }

        } catch (error) {
            console.error('Registration error:', error);
            displayMessage(registerMessageDiv, error.message || 'Registration failed. Please try again.', 'error');
        } finally {
            if(submitButton) submitButton.disabled = false;
        }
    }

    // --- OTP VERIFICATION FUNCTION ---
    async function handleOtpVerification(event) {
        event.preventDefault();
        const form = event.target;
        // Get email from the hidden input if OTP form is on same page, or from URL if separate page
        const email = otpVerificationEmailInput ? otpVerificationEmailInput.value : new URLSearchParams(window.location.search).get('email');
        const otp = form.otp.value;
        const submitButton = form.querySelector('button[type="submit"]');

        clearMessages(otpMessageDiv);
        if(submitButton) submitButton.disabled = true;

        if (!email) {
            displayMessage(otpMessageDiv, "Email is missing for OTP verification. Please try registering again.", "error");
            if(submitButton) submitButton.disabled = false;
            return;
        }

        try {
            await makeApiCall('/api/auth/verify-otp', 'POST', { email, otp }, {}, true, "Verifying OTP...");
            displayMessage(otpMessageDiv, 'Email verified successfully! Redirecting to login...', 'success');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
        } catch (error) {
            console.error('OTP Verification error:', error);
            let friendlyMessage = 'OTP verification failed. Please try again.';
             if (error.details && error.details.errorCode) {
                switch (error.details.errorCode) {
                    case "INVALID_OTP": friendlyMessage = "The OTP you entered is incorrect. Please try again."; break;
                    case "OTP_EXPIRED": friendlyMessage = "The OTP has expired. Please request a new one."; break;
                    default: friendlyMessage = error.details.message || error.message || friendlyMessage;
                }
            } else if (error.message) {
                friendlyMessage = error.message;
            }
            displayMessage(otpMessageDiv, friendlyMessage, 'error');
        } finally {
            if(submitButton) submitButton.disabled = false;
        }
    }

    // --- RESEND OTP FUNCTION ---
    async function handleResendOtp(event) {
        event.preventDefault();
        const emailToResend = otpVerificationEmailInput ? otpVerificationEmailInput.value : new URLSearchParams(window.location.search).get('email');

        if (!emailToResend) {
            displayMessage(otpMessageDiv, 'Could not identify email to resend OTP. Please ensure you started the registration process.', 'error');
            return;
        }
        clearMessages(otpMessageDiv);
        if(resendOtpLink) resendOtpLink.style.pointerEvents = 'none';

        try {
            const message = await makeApiCall('/api/auth/resend-otp', 'POST', { email: emailToResend }, {}, true, "Resending OTP...");
            displayMessage(otpMessageDiv, message || 'New OTP sent. Please check your email.', 'success');
        } catch (error) {
            console.error('Resend OTP error:', error);
            displayMessage(otpMessageDiv, error.message || 'Failed to resend OTP. Please try again.', 'error');
        } finally {
            if(resendOtpLink) setTimeout(() => { resendOtpLink.style.pointerEvents = 'auto'; }, 5000);
        }
    }

    // --- HELPER FUNCTIONS ---
    function toggleEmployeeSpecificFields(roleValue, mainEmployeeFieldsContainer, currentForm) {
        // This 'currentForm' is expected to be the registerForm element
        console.log(`toggleEmployeeSpecificFields called for role: '${roleValue}'`); // DEBUG

        if (!mainEmployeeFieldsContainer) {
            console.error("toggleEmployeeSpecificFields: mainEmployeeFieldsContainer (div#employeeSpecificFields) is null!");
            return;
        }
        if (!currentForm) {
            console.error("toggleEmployeeSpecificFields: currentForm (registerForm element) is null!");
            return;
        }

        const adminSelectionGroup = document.getElementById('adminSelectionGroup'); // The div wrapping the admin select
        const managedByAdminField = currentForm.managedByAdminId; // The <select name="managedByAdminId">
        const dateOfBirthField = currentForm.dateOfBirth;
        const hireDateField = currentForm.hireDate;
        const salaryField = currentForm.salary;
        const departmentIdField = currentForm.departmentId;

        // Ensure these elements are actually found
        // console.log({managedByAdminField, dateOfBirthField, hireDateField, salaryField, departmentIdField, adminSelectionGroup});


        if (roleValue === 'EMPLOYEE') {
            mainEmployeeFieldsContainer.style.display = 'block';
            console.log("Displaying #employeeSpecificFields block"); // DEBUG

            if (adminSelectionGroup) {
                adminSelectionGroup.style.display = 'block';
                console.log("Displaying #adminSelectionGroup block"); // DEBUG
            } else {
                console.warn("#adminSelectionGroup element not found!");
            }

            if (managedByAdminField) {
                managedByAdminField.required = true; // Manager is required for employee self-registration
                console.log("managedByAdminId field set to required"); // DEBUG
            } else {
                console.warn("managedByAdminId select field not found in form!");
            }

            // Your HTML already marks these as required. This JS logic re-affirms it or can change it.
            // For employee self-reg, decide if these are truly mandatory or optional.
            // Example: making them required when the section is visible.
            if (dateOfBirthField) dateOfBirthField.required = true;
            if (hireDateField) hireDateField.required = true;
            if (salaryField) salaryField.required = true;
            if (departmentIdField) departmentIdField.required = true;
            console.log("Other employee detail fields 'required' attributes set for EMPLOYEE role.");

        } else { // For ADMIN or if roleValue is empty/null
            mainEmployeeFieldsContainer.style.display = 'none';
            console.log("Hiding #employeeSpecificFields block"); // DEBUG

            // adminSelectionGroup is inside mainEmployeeFieldsContainer, so it will hide too.
            // But we still need to manage the 'required' attribute of the select if it exists.
            if (managedByAdminField) managedByAdminField.required = false;
            if (dateOfBirthField) dateOfBirthField.required = false;
            if (hireDateField) hireDateField.required = false;
            if (salaryField) salaryField.required = false;
            if (departmentIdField) departmentIdField.required = false;
            console.log("'required' attributes removed from employee detail fields.");
        }
    }

   async function populateAdminManagersDropdown(selectElement) {
       if (!selectElement) {
           console.warn("populateAdminManagersDropdown: selectElement (managedByAdminId) is null");
           return;
       }
       console.log("Populating admin managers dropdown for element:", selectElement.id); // Existing debug

       // Initial state while loading
       selectElement.innerHTML = '<option value="" disabled selected>Loading managers...</option>';
       selectElement.disabled = true; // Disable while loading

       try {
           const admins = await makeApiCall('/api/admins/list', 'GET', null, {}, false); // showLoader = false
           console.log("Admins fetched for dropdown:", admins); // Existing debug

           if (admins && Array.isArray(admins)) {
               if (admins.length === 0) {
                   selectElement.innerHTML = '<option value="" disabled selected>No managers available</option>';
                   // Keep disabled as there are no options to select
               } else {
                   // Clear previous options and add the placeholder again before populating
                   selectElement.innerHTML = '<option value="" disabled selected>Select your manager/admin</option>';
                   admins.forEach(admin => {
                       const option = document.createElement('option');
                       option.value = admin.id; // The value of the option will be the admin's ID
                       // **** THIS IS THE CHANGE ****
                       option.textContent = `${admin.fullName} - ${admin.id}`; // Format: "FullName - ID"
                       selectElement.appendChild(option);
                   });
                   selectElement.disabled = false; // Enable the dropdown now that it has options
               }
           } else {
               // This case means 'admins' was null, undefined, or not an array (API issue or parsing issue in makeApiCall)
               selectElement.innerHTML = '<option value="" disabled selected>Could not load managers</option>';
               console.warn('No admin managers loaded or invalid format from API. Response:', admins);
               // Keep disabled
           }
       } catch (error) {
           console.error('Error populating admin managers dropdown:', error);
           selectElement.innerHTML = '<option value="" disabled selected>Error loading managers</option>';
           // Keep disabled
       }
   }

       // SCENARIO 2 NEW/MODIFIED FUNCTION: Populate departments based on selected Admin
       async function populateDepartmentsForSelectedAdmin(departmentSelectElement, selectedAdminId) {
           if (!departmentSelectElement) {
               console.warn("populateDepartmentsForSelectedAdmin: departmentSelectElement is null");
               return;
           }

           console.log(`Populating departments for selected admin ID: ${selectedAdminId}`);
           departmentSelectElement.innerHTML = '<option value="" disabled selected>Loading departments...</option>';
           departmentSelectElement.disabled = true;

           try {
               // API endpoint should be like /api/departments?adminId=VALUE
               // Ensure your DepartmentController's GET /api/departments handles this query parameter
               const departments = await makeApiCall(`/api/departments/all`, 'GET', null, {}, false);
               console.log("Departments for selected admin:", departments);

               // Department is optional for employee self-registration
               departmentSelectElement.innerHTML = '<option value="" selected>Select department</option>';
               departmentSelectElement.required = false; // Explicitly optional

               if (departments && Array.isArray(departments) && departments.length > 0) {
                   departments.forEach(dept => {
                       const option = document.createElement('option');
                       option.value = dept.id;
                       option.textContent = dept.name;
                       departmentSelectElement.appendChild(option);
                   });
               } else {
                    departmentSelectElement.innerHTML = '<option value="" selected>No departments for this manager (optional)</option>';
               }
               departmentSelectElement.disabled = false; // Enable after populating
           } catch (error) {
               console.error('Error populating departments for selected admin:', error);
               departmentSelectElement.innerHTML = '<option value="" selected>Error loading departments (optional)</option>';
               departmentSelectElement.disabled = false;
           }
       }

    // displayMessage and clearMessages (ensure these are correctly defined as per previous versions)
    function displayMessage(element, message, type = 'info', allowHtml = false) {
        if (!element) {
            console.warn("displayMessage: Target element is null for message:", message);
            return;
        }
        if (allowHtml) {
            element.innerHTML = message;
        } else {
            element.textContent = message;
        }
        element.className = 'message-area';
        if (type === 'error') element.classList.add('error-message');
        else if (type === 'success') element.classList.add('success-message');
        else element.classList.add('info-message');
        element.style.display = 'block';

        if ((type === 'success' || type === 'info') && !allowHtml) {
            setTimeout(() => { clearMessage(element); }, 5000);
        }
    }

    function clearMessages(...elements) {
        elements.forEach(element => {
            if (element) {
                element.textContent = '';
                element.style.display = 'none';
                element.className = 'message-area';
            }
        });
    }
});