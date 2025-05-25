document.addEventListener('DOMContentLoaded', function() {
    const loggedInUser = getLoggedInUser();
    if (!loggedInUser || loggedInUser.role !== 'EMPLOYEE') {
        // Redirect or show error if not an employee (or not logged in)
        // For apply_leave.html and my_leaves.html, this check is important.
        // If common.js handles global redirection for non-logged-in users, that's fine.
        // This is an additional layer for role-specific pages.
        if (window.location.pathname.includes('apply_leave.html') || window.location.pathname.includes('my_leaves.html')) {
            alert('Access Denied. Please log in as an Employee.');
            window.location.href = 'login.html';
            return;
        }
    }

    // --- Apply Leave Page Logic ---
    const applyLeaveForm = document.getElementById('applyLeaveForm');
    const leaveFormMessage = document.getElementById('leaveFormMessage');

    if (applyLeaveForm) {
        applyLeaveForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            clearMessage(leaveFormMessage);
            this.querySelector('button[type="submit"]').disabled = true;

            const leaveData = {
                employeeId: loggedInUser.id, // Get ID from logged-in user
                startDate: document.getElementById('leaveStartDate').value,
                endDate: document.getElementById('leaveEndDate').value,
                reason: document.getElementById('leaveReason').value.trim()
            };

            try {
                await makeApiCall('/api/leaves/apply', 'POST', leaveData, {}, true, "Submitting request...");
                displayMessage(leaveFormMessage, 'Leave request submitted successfully! You will be notified of updates.', 'success');
                applyLeaveForm.reset();
            } catch (error) {
                console.error("Error applying for leave:", error);
                displayMessage(leaveFormMessage, error.message || "Failed to submit leave request.", 'error');
            } finally {
                this.querySelector('button[type="submit"]').disabled = false;
            }
        });
    }

    // --- My Leave Requests Page Logic ---
    const myLeavesTableBody = document.getElementById('myLeavesTableBody');
    const myLeavesTable = document.getElementById('myLeavesTable');
    const loadingMyLeavesMsg = document.getElementById('loadingMyLeavesMsg');
    const myLeaveListMessage = document.getElementById('myLeaveListMessage');

    if (myLeavesTableBody) { // Indicates we are on my_leaves.html
        fetchMyLeaveRequests();
    }

    async function fetchMyLeaveRequests() {
        if (!loggedInUser || !loggedInUser.id) {
            displayMessage(myLeaveListMessage, "User not logged in.", "error");
            return;
        }
        showLoadingMyLeaves(true);
        clearMessage(myLeaveListMessage);

        try {
            const requests = await makeApiCall(`/api/leaves/my-requests/${loggedInUser.id}`, 'GET', null, {}, true, "Fetching requests...");
            renderMyLeavesTable(requests);
            if (!requests || requests.length === 0) {
                displayMessage(myLeaveListMessage, "You have not submitted any leave requests yet.", "info");
            }
        } catch (error) {
            console.error("Error fetching my leave requests:", error);
            displayMessage(myLeaveListMessage, error.message || "Failed to load your leave requests.", "error");
            renderMyLeavesTable([]); // Clear table
        } finally {
            showLoadingMyLeaves(false);
        }
    }

    function renderMyLeavesTable(requests) {
        myLeavesTableBody.innerHTML = '';
        if (requests && requests.length > 0) {
            myLeavesTable.style.display = '';
            requests.forEach(req => {
                const row = myLeavesTableBody.insertRow();
                row.insertCell().textContent = req.id;
                row.insertCell().textContent = formatDate(req.startDate);
                row.insertCell().textContent = formatDate(req.endDate);
                row.insertCell().textContent = req.reason || 'N/A';

                const statusCell = row.insertCell();
                const statusBadge = document.createElement('span');
                statusBadge.className = `status-badge status-${req.status}`;
                statusBadge.textContent = req.status;
                statusCell.appendChild(statusBadge);

                row.insertCell().textContent = formatDate(req.createdAt, true); // Show time for submitted
                row.insertCell().textContent = req.adminRemarks || 'N/A';

                const actionCell = row.insertCell();
                if (req.status === 'PENDING') {
                    const cancelButton = document.createElement('button');
                    cancelButton.textContent = 'Cancel';
                    cancelButton.classList.add('btn', 'btn-danger-outline', 'btn-sm'); // Or a different style
                    cancelButton.onclick = () => cancelMyLeave(req.id);
                    actionCell.appendChild(cancelButton);
                } else {
                    actionCell.textContent = '-';
                }
            });
        } else {
            myLeavesTable.style.display = 'none';
        }
    }

    async function cancelMyLeave(leaveId) {
        if (!confirm("Are you sure you want to cancel this leave request?")) return;
        clearMessage(myLeaveListMessage);
        try {
            await makeApiCall(`/api/leaves/my-requests/${leaveId}/cancel/${loggedInUser.id}`, 'PUT', null, {}, true, "Cancelling request...");
            displayMessage(myLeaveListMessage, "Leave request cancelled successfully.", "success");
            fetchMyLeaveRequests(); // Refresh the list
        } catch (error) {
            console.error("Error cancelling leave request:", error);
            displayMessage(myLeaveListMessage, error.message || "Failed to cancel leave request.", "error");
        }
    }


    function showLoadingMyLeaves(isLoading) {
        if (loadingMyLeavesMsg) loadingMyLeavesMsg.style.display = isLoading ? 'block' : 'none';
        if (myLeavesTable) myLeavesTable.style.display = isLoading ? 'none' : (myLeavesTableBody.rows.length > 0 ? '' : 'none');
    }

    // Helper: Format date (consistent with other JS files if you have one there)
    function formatDate(dateStringOrArray, includeTime = false) {
        if (!dateStringOrArray) return 'N/A';
        try {
            let dateToFormat;
            if (Array.isArray(dateStringOrArray) && dateStringOrArray.length >= 3) {
                dateToFormat = new Date(dateStringOrArray[0], dateStringOrArray[1] - 1, dateStringOrArray[2],
                                        dateStringOrArray[3] || 0, dateStringOrArray[4] || 0, dateStringOrArray[5] || 0);
            } else if (typeof dateStringOrArray === 'string') {
                dateToFormat = new Date(dateStringOrArray);
            } else { return String(dateStringOrArray); }

            if (isNaN(dateToFormat.getTime())) return String(dateStringOrArray); // Invalid date

            const options = { year: 'numeric', month: 'short', day: 'numeric' };
            if (includeTime) {
                options.hour = '2-digit';
                options.minute = '2-digit';
            }
            return dateToFormat.toLocaleDateString(undefined, options);
        } catch (e) {
            console.warn("Could not format date:", dateStringOrArray, e);
            return String(dateStringOrArray);
        }
    }

    // --- Common displayMessage and clearMessages (can be moved to common.js if not already specialized) ---
    function displayMessage(element, message, type = 'info') {
        if (element) {
            element.textContent = message;
            element.className = 'message-area';
            if (type === 'error') element.classList.add('error-message');
            else if (type === 'success') element.classList.add('success-message');
            else element.classList.add('info-message');
            element.style.display = 'block';
            if (type !== 'error') setTimeout(() => { clearMessage(element); }, 5000);
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