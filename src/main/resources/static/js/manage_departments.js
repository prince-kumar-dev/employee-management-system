document.addEventListener('DOMContentLoaded', function() {
    const loggedInUser = getLoggedInUser(); // From common.js
    if (!loggedInUser || loggedInUser.role !== 'ADMIN' || !loggedInUser.id) { // Also check for ID
        alert('Access Denied or Admin ID missing. Please log in as an Admin.');
        window.location.href = 'login.html';
        return;
    }

    const departmentForm = document.getElementById('departmentForm');
    const departmentIdInput = document.getElementById('departmentId');
    const departmentNameInput = document.getElementById('departmentName');
    const formTitle = document.getElementById('formTitle');
    const saveDepartmentBtn = document.getElementById('saveDepartmentBtn');
    const cancelEditBtn = document.getElementById('cancelEditBtn');
    const departmentsTableBody = document.getElementById('departmentsTableBody');
    const departmentsTable = document.getElementById('departmentsTable');
    const loadingMsg = document.getElementById('loadingDepartmentsMsg');
    const departmentFormMessage = document.getElementById('departmentFormMessage');
    const departmentListMessage = document.getElementById('departmentListMessage');

    const adminHeaders = { 'X-Admin-Id': String(loggedInUser.id) }; // Define headers once for reuse

    // Load initial departments
    fetchDepartments();

    departmentForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        saveDepartmentBtn.disabled = true;
        clearMessage(departmentFormMessage);

        const id = departmentIdInput.value;
        const name = departmentNameInput.value.trim();

        if (!name) {
            displayMessage(departmentFormMessage, 'Department name cannot be empty.', 'error');
            saveDepartmentBtn.disabled = false;
            return;
        }

        const departmentData = { name };
        let url = '/api/departments';
        let method = 'POST';

        if (id) { // If ID exists, it's an update
            url += `/${id}`;
            method = 'PUT';
        }

        try {
            // **** FIX: ADD adminHeaders to makeApiCall for POST/PUT ****
            const result = await makeApiCall(url, method, departmentData, adminHeaders, true, id ? "Updating department..." : "Adding department...");
            displayMessage(departmentFormMessage, `Department ${id ? 'updated' : 'added'} successfully!`, 'success');
            await fetchDepartments(); // Refresh the list
            resetForm();
        } catch (error) {
            console.error('Error saving department:', error);
            displayMessage(departmentFormMessage, error.message || `Failed to ${id ? 'update' : 'add'} department.`, 'error');
        } finally {
            saveDepartmentBtn.disabled = false;
        }
    });

    cancelEditBtn.addEventListener('click', function() {
        resetForm();
    });

    async function fetchDepartments() {
        showLoading(true);
        clearMessage(departmentListMessage);

        // adminHeaders is already defined globally in this script scope
        try {
            const departments = await makeApiCall('/api/departments/my-managed', 'GET', null, adminHeaders, true, "Loading departments...");
            renderDepartmentsTable(departments || []); // Ensure an array is passed
            if (!departments || departments.length === 0) {
                 displayMessage(departmentListMessage, 'No departments found. Add one using the form above.', 'info');
            }
        } catch (error) {
            console.error('Error fetching departments:', error);
            displayMessage(departmentListMessage, 'Failed to load departments. ' + (error.message || ''), 'error');
            renderDepartmentsTable([]); // Clear table on error
        } finally {
            showLoading(false);
        }
    }

    function renderDepartmentsTable(departments) {
        departmentsTableBody.innerHTML = '';
        if (departments && departments.length > 0) {
            departmentsTable.style.display = '';
            clearMessage(departmentListMessage); // Clear "no departments" message
            departments.forEach(dept => {
                const row = departmentsTableBody.insertRow();
                row.insertCell().textContent = dept.id;
                row.insertCell().textContent = dept.name;
                // If you add createdByAdminName to DepartmentDto and want to display it:
                // row.insertCell().textContent = dept.createdByAdminName || 'N/A';


                const actionsCell = row.insertCell();
                actionsCell.classList.add('actions-cell');

                const editButton = document.createElement('button');
                editButton.textContent = 'Edit';
                editButton.classList.add('btn', 'btn-edit', 'btn-sm');
                editButton.onclick = () => loadDepartmentForEdit(dept);
                actionsCell.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Delete';
                deleteButton.classList.add('btn', 'btn-delete', 'btn-sm');
                deleteButton.onclick = () => deleteDepartment(dept.id, dept.name);
                actionsCell.appendChild(deleteButton);
            });
        } else {
            departmentsTable.style.display = 'none';
        }
    }

    function loadDepartmentForEdit(department) {
        formTitle.textContent = 'Edit Department';
        departmentIdInput.value = department.id;
        departmentNameInput.value = department.name;
        saveDepartmentBtn.textContent = 'Update Department';
        cancelEditBtn.style.display = 'inline-block';
        departmentNameInput.focus();
        window.scrollTo(0, 0);
        clearMessage(departmentFormMessage);
    }

    async function deleteDepartment(id, name) {
        clearMessage(departmentListMessage); // Clear previous messages
        if (confirm(`Are you sure you want to delete the department "${name}" (ID: ${id})? This action cannot be undone.`)) {
            try {
                // **** FIX: ADD adminHeaders to makeApiCall for DELETE ****
                await makeApiCall(`/api/departments/${id}`, 'DELETE', null, adminHeaders, true, "Deleting department...");
                displayMessage(departmentListMessage, `Department "${name}" deleted successfully.`, 'success');
                await fetchDepartments(); // Refresh list
            } catch (error) {
                console.error('Error deleting department:', error);
                displayMessage(departmentListMessage, error.message || `Failed to delete department "${name}".`, 'error');
            }
        }
    }

    function resetForm() {
        formTitle.textContent = 'Add New Department';
        departmentForm.reset();
        departmentIdInput.value = '';
        saveDepartmentBtn.textContent = 'Save Department';
        cancelEditBtn.style.display = 'none';
        clearMessage(departmentFormMessage);
        saveDepartmentBtn.disabled = false; // Ensure button is re-enabled
    }

    function showLoading(isLoading) {
        if (loadingMsg) {
            loadingMsg.style.display = isLoading ? 'block' : 'none';
        }
        if (departmentsTable) {
            departmentsTable.style.display = isLoading ? 'none' : (departmentsTableBody.rows.length > 0 ? '' : 'none');
        }
    }

    function displayMessage(element, message, type = 'info') {
        if (element) {
            element.textContent = message;
            element.className = 'message-area';
            if (type === 'error') element.classList.add('error-message');
            else if (type === 'success') element.classList.add('success-message');
            else element.classList.add('info-message');
            element.style.display = 'block';
            if (type === 'success' || type === 'info') {
                setTimeout(() => { clearMessage(element); }, 5000);
            }
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