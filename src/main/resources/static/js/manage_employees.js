document.addEventListener('DOMContentLoaded', function() {
    const loggedInUser = getLoggedInUser();
    if (!loggedInUser || loggedInUser.role !== 'ADMIN') {
        alert('Access Denied. You must be an Admin to view this page.');
        window.location.href = 'login.html';
        return;
    }

    // Form elements
    const employeeForm = document.getElementById('employeeForm');
    const employeeFormTitle = document.getElementById('employeeFormTitle');
    const employeeIdInput = document.getElementById('employeeId');
    const firstNameInput = document.getElementById('firstName');
    const lastNameInput = document.getElementById('lastName');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const genderSelect = document.getElementById('gender'); // Explicitly declare genderSelect
    const dateOfBirthInput = document.getElementById('dateOfBirth');
    const hireDateInput = document.getElementById('hireDate');
    const salaryInput = document.getElementById('salary');
    const departmentIdSelect = document.getElementById('departmentId');
    const roleSelect = document.getElementById('role');
    const saveEmployeeBtn = document.getElementById('saveEmployeeBtn');
    const cancelEmployeeEditBtn = document.getElementById('cancelEmployeeEditBtn');
    const employeeFormMessage = document.getElementById('employeeFormMessage');

    // List elements
    const employeesTableBody = document.getElementById('employeesTableBody');
    const employeesTable = document.getElementById('employeesTable');
    const loadingEmployeesMsg = document.getElementById('loadingEmployeesMsg');
    const employeeListMessage = document.getElementById('employeeListMessage');
    const departmentFilterSelect = document.getElementById('departmentFilter');

    let allDepartments = [];

    const adminHeaders = { 'X-Admin-Id': String(loggedInUser.id) };

    loadDepartmentsAndThenEmployees();

    async function loadDepartmentsAndThenEmployees() {
        try {
            allDepartments = await makeApiCall('/api/departments/my-managed', 'GET', null, adminHeaders, false);
            populateDepartmentDropdown(departmentIdSelect, allDepartments, "Select Department", true);
            populateDepartmentDropdown(departmentFilterSelect, allDepartments, "All Departments");
            await fetchEmployees();
        } catch (error) {
            console.error("Error loading departments:", error);
            displayMessage(employeeListMessage, "Failed to load department data for form/filters. " + (error.message || ''), "error");
            showLoading(false);
        }
    }

    function populateDepartmentDropdown(selectElement, departments, defaultOptionText = "Select Department", isRequired = false) {
        if (!selectElement) return;
        selectElement.innerHTML = `<option value="" ${isRequired ? 'disabled selected' : 'selected'}>${defaultOptionText}</option>`;
        if (departments && departments.length > 0) {
            departments.forEach(dept => {
                const option = document.createElement('option');
                option.value = dept.id;
                option.textContent = dept.name;
                selectElement.appendChild(option);
            });
        }
    }

    departmentFilterSelect.addEventListener('change', fetchEmployees);

    employeeForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        saveEmployeeBtn.disabled = true;
        clearMessage(employeeFormMessage);

        const id = employeeIdInput.value;

        if (!id && !passwordInput.value.trim()) {
            displayMessage(employeeFormMessage, 'Password is required for new employees.', 'error');
            saveEmployeeBtn.disabled = false;
            return;
        }
        if (genderSelect.value === "") { // Specific check for mandatory gender
            displayMessage(employeeFormMessage, 'Gender is required.', 'error');
            saveEmployeeBtn.disabled = false;
            return;
        }

        const employeeData = {
            firstName: firstNameInput.value.trim(),
            lastName: lastNameInput.value.trim(),
            email: emailInput.value.trim(),
            gender: genderSelect.value, // Use the declared variable
            dateOfBirth: dateOfBirthInput.value || null,
            hireDate: hireDateInput.value,
            salary: salaryInput.value ? parseFloat(salaryInput.value) : null,
            departmentId: departmentIdSelect.value ? parseInt(departmentIdSelect.value) : null,
            role: roleSelect.value
        };
        if (passwordInput.value.trim()) {
            employeeData.password = passwordInput.value.trim();
        }

        let url = '/api/employees';
        let method = 'POST';

        if (id) {
            url += `/${id}`;
            method = 'PUT';
        }

        try {
            await makeApiCall(url, method, employeeData, adminHeaders, true, id ? "Updating employee..." : "Adding employee...");
            displayMessage(employeeFormMessage, `Employee ${id ? 'updated' : 'added'} successfully! ${!id ? 'A welcome email has been sent.' : ''}`, 'success');
            await fetchEmployees();
            resetEmployeeForm();
        } catch (error) {
            console.error('Error saving employee:', error);
            displayMessage(employeeFormMessage, error.message || `Failed to ${id ? 'update' : 'add'} employee.`, 'error');
        } finally {
            saveEmployeeBtn.disabled = false;
        }
    });

    cancelEmployeeEditBtn.addEventListener('click', function() {
        resetEmployeeForm();
    });

    async function fetchEmployees() {
        showLoading(true);
        clearMessage(employeeListMessage);
        let url = '/api/employees';
        const selectedDepartmentId = departmentFilterSelect.value;

        const loggedInAdmin = getLoggedInUser(); // This is your logged-in admin
        if (!loggedInAdmin || !loggedInAdmin.id) {
            displayMessage(employeeListMessage, "Admin not properly logged in.", "error");
            showLoadingState(false);
            return;
        }

        const headers = { 'X-Admin-Id': String(loggedInAdmin.id) }; // Create headers object

        try {
            const employees = await makeApiCall(url, 'GET', null, headers, true, "Loading employees...");
            let filteredEmployees = employees || [];
            if (selectedDepartmentId) {
                filteredEmployees = filteredEmployees.filter(emp => emp.departmentId == selectedDepartmentId);
            }
            renderEmployeesTable(filteredEmployees);

            if (!filteredEmployees || filteredEmployees.length === 0) {
                 displayMessage(employeeListMessage, 'No employees found matching the criteria.', 'info');
            }
        } catch (error) {
            console.error('Error fetching employees:', error);
            renderEmployeesTable([]);
            displayMessage(employeeListMessage, 'Failed to load employees. ' + (error.message || ''), 'error');
        } finally {
            showLoading(false);
        }
    }

    function renderEmployeesTable(employees) {
        employeesTableBody.innerHTML = '';
        if (employees && employees.length > 0) {
            employeesTable.style.display = '';
            clearMessage(employeeListMessage);
            employees.forEach(emp => {
                const row = employeesTableBody.insertRow();
                row.insertCell().textContent = emp.id;
                row.insertCell().textContent = `${emp.firstName} ${emp.lastName}`;
                row.insertCell().textContent = emp.email;
                const dept = allDepartments.find(d => d.id === emp.departmentId);
                row.insertCell().textContent = dept ? dept.name : 'N/A';
                row.insertCell().textContent = emp.role;
                row.insertCell().textContent = emp.gender || 'N/A'; // Display Gender
                row.insertCell().textContent = emp.hireDate ? formatDate(emp.hireDate) : 'N/A';

                const actionsCell = row.insertCell();
                actionsCell.classList.add('actions-cell');

                const editButton = document.createElement('button');
                editButton.textContent = 'Edit';
                editButton.classList.add('btn', 'btn-edit', 'btn-sm');
                editButton.onclick = () => loadEmployeeForEdit(emp);
                actionsCell.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Delete';
                deleteButton.classList.add('btn', 'btn-delete', 'btn-sm');
                deleteButton.onclick = () => deleteEmployee(emp.id, `${emp.firstName} ${emp.lastName}`);
                actionsCell.appendChild(deleteButton);
            });
        } else {
            employeesTable.style.display = 'none';
        }
    }

    function formatDate(dateStringOrArray) {
        if (!dateStringOrArray) return 'N/A';
        try {
            if (Array.isArray(dateStringOrArray)) {
                const [year, month, day] = dateStringOrArray;
                return new Date(year, month - 1, day).toLocaleDateString();
            } else if (typeof dateStringOrArray === 'string') {
                return new Date(dateStringOrArray.split('T')[0]).toLocaleDateString();
            }
            return String(dateStringOrArray);
        } catch (e) {
            console.warn("Could not format date:", dateStringOrArray, e);
            return String(dateStringOrArray);
        }
    }

    function loadEmployeeForEdit(employee) {
        employeeFormTitle.textContent = 'Edit Employee';
        employeeIdInput.value = employee.id;
        firstNameInput.value = employee.firstName;
        lastNameInput.value = employee.lastName;
        emailInput.value = employee.email;
        passwordInput.value = '';
        document.getElementById('passwordHelp').textContent = 'Leave blank if not changing password.';

        genderSelect.value = employee.gender || ""; // Set gender for edit using the declared variable

        dateOfBirthInput.value = employee.dateOfBirth ? convertToInputDate(employee.dateOfBirth) : '';
        hireDateInput.value = employee.hireDate ? convertToInputDate(employee.hireDate) : '';
        salaryInput.value = employee.salary || '';
        departmentIdSelect.value = employee.departmentId || '';
        roleSelect.value = employee.role;

        saveEmployeeBtn.textContent = 'Update Employee';
        cancelEmployeeEditBtn.style.display = 'inline-block';
        firstNameInput.focus();
        window.scrollTo(0, 0);
        clearMessage(employeeFormMessage);
    }

    function convertToInputDate(dateValue){
        if (!dateValue) return '';
        let dateObj;
        if (Array.isArray(dateValue)) {
            dateObj = new Date(dateValue[0], dateValue[1] - 1, dateValue[2]);
        } else {
            dateObj = new Date(dateValue.split('T')[0]);
        }
        const year = dateObj.getFullYear();
        const month = ('0' + (dateObj.getMonth() + 1)).slice(-2);
        const day = ('0' + dateObj.getDate()).slice(-2);
        return `${year}-${month}-${day}`;
    }

    async function deleteEmployee(id, name) {
        clearMessage(employeeListMessage);
        if (confirm(`Are you sure you want to delete employee "${name}" (ID: ${id})?`)) {
            try {
                await makeApiCall(`/api/employees/${id}`, 'DELETE', null, adminHeaders, true, "Deleting employee...");
                displayMessage(employeeListMessage, `Employee "${name}" deleted successfully.`, 'success');
                await fetchEmployees();
            } catch (error) {
                console.error('Error deleting employee:', error);
                displayMessage(employeeListMessage, error.message || `Failed to delete employee "${name}".`, 'error');
            }
        }
    }

    function resetEmployeeForm() {
        employeeFormTitle.textContent = 'Add New Employee';
        employeeForm.reset(); // This will reset genderSelect to its first option (the disabled "Select Gender")
        employeeIdInput.value = '';
        document.getElementById('passwordHelp').textContent = 'Required for new employees. Leave blank if not changing for existing employee.';
        saveEmployeeBtn.textContent = 'Save Employee';
        cancelEmployeeEditBtn.style.display = 'none';
        clearMessage(employeeFormMessage);
        saveEmployeeBtn.disabled = false;
    }

    function showLoading(isLoading) {
        if (isLoading) {
            if(loadingEmployeesMsg) loadingEmployeesMsg.style.display = 'block';
            if(employeesTable) employeesTable.style.display = 'none';
        } else {
            if(loadingEmployeesMsg) loadingEmployeesMsg.style.display = 'none';
        }
    }

    function displayMessage(element, message, type = 'info', allowHtml = false) {
        if (!element) {
            console.warn("Attempted to display message on a null element for message:", message);
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

    function clearMessage(element) {
        if (element) {
            element.textContent = '';
            element.style.display = 'none';
            element.className = 'message-area';
        }
    }
});