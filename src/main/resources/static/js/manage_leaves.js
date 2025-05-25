document.addEventListener('DOMContentLoaded', function() {
    const loggedInUser = getLoggedInUser();
    if (!loggedInUser || loggedInUser.role !== 'ADMIN') {
        alert('Access Denied. You must be an Admin to view this page.');
        window.location.href = 'login.html';
        return;
    }

    const allLeavesTableBody = document.getElementById('allLeavesTableBody');
    const allLeavesTable = document.getElementById('allLeavesTable');
    const loadingAllLeavesMsg = document.getElementById('loadingAllLeavesMsg');
    const allLeaveListMessage = document.getElementById('allLeaveListMessage');
    const filterButtons = document.querySelectorAll('.leave-filters .btn');

    // Modal elements
    const leaveActionModal = document.getElementById('leaveActionModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalEmployeeName = document.getElementById('modalEmployeeName');
    const modalLeaveDates = document.getElementById('modalLeaveDates');
    const modalLeaveReason = document.getElementById('modalLeaveReason');
    const adminRemarksInput = document.getElementById('adminRemarks');
    const approveLeaveBtn = document.getElementById('approveLeaveBtn');
    const rejectLeaveBtn = document.getElementById('rejectLeaveBtn');
    const cancelModalBtn = document.getElementById('cancelModalBtn');
    const actionLeaveIdInput = document.getElementById('actionLeaveId');
    const leaveActionMessage = document.getElementById('leaveActionMessage');

    let currentFilterStatus = "";

    // Initial fetch
    fetchLeaveRequestsByStatus(currentFilterStatus);

    filterButtons.forEach(button => {
        button.addEventListener('click', function() {
            filterButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');
            currentFilterStatus = this.dataset.status;
            fetchLeaveRequestsByStatus(currentFilterStatus);
        });
    });

    async function fetchLeaveRequestsByStatus(status = "") {
        showLoadingState(true); // Unified loading state management
        clearMessage(allLeaveListMessage);
        let url = `/api/leaves/admin/all`;
        if (status) {
            url += `?status=${status}`;
        }

        const headers = { 'X-Admin-Id': String(loggedInUser.id) };

        try {
            // Use the global loader from common.js by default for makeApiCall
            // Set the 4th parameter (showLoader) to false if you want to manage loader locally only.
            const requests = await makeApiCall(url, 'GET', null, headers, true, "Fetching leave requests...");

            console.log("Fetched leave requests from API:", requests); // DEBUG: Crucial log

            // Ensure 'requests' is an array. If makeApiCall returns null for non-JSON success,
            // and your API /api/leaves/admin/all IS returning JSON, this means makeApiCall might be misinterpreting.
            // However, if the API genuinely might return nothing (e.g. 204, or non-JSON), then `|| []` is a safe default.
            renderAllLeavesTable(requests || []); // Always pass an array

            if (!requests || requests.length === 0) {
                displayMessage(allLeaveListMessage, "No leave requests found for the selected filter.", "info");
            }
        } catch (error) {
            console.error("Error fetching leave requests:", error);
            displayMessage(allLeaveListMessage, error.message || "Failed to load leave requests.", "error");
            renderAllLeavesTable([]); // Clear/hide table on error
        } finally {
            showLoadingState(false); // Unified loading state management
        }
    }

    function renderAllLeavesTable(requestsArray) { // Ensure parameter is named descriptively
        allLeavesTableBody.innerHTML = ''; // Clear previous rows

        if (requestsArray && requestsArray.length > 0) {
            allLeavesTable.style.display = ''; // Show table
            clearMessage(allLeaveListMessage); // Clear any "no requests" message if data is now present

            requestsArray.forEach(req => {
                try { // Add try-catch for individual row rendering to catch specific data issues
                    const row = allLeavesTableBody.insertRow();
                    row.insertCell().textContent = req.id !== null && req.id !== undefined ? req.id : 'N/A';
                    row.insertCell().textContent = req.employeeName || 'N/A';
                    row.insertCell().textContent = req.employeeEmail || 'N/A';
                    row.insertCell().textContent = formatDate(req.startDate);
                    row.insertCell().textContent = formatDate(req.endDate);
                    row.insertCell().textContent = req.reason || 'N/A';

                    const statusCell = row.insertCell();
                    const statusBadge = document.createElement('span');
                    statusBadge.className = `status-badge status-${req.status || 'UNKNOWN'}`; // Handle undefined status
                    statusBadge.textContent = req.status || 'UNKNOWN';
                    statusCell.appendChild(statusBadge);

                    row.insertCell().textContent = formatDate(req.createdAt, true);

                    const actionsCell = row.insertCell();
                    if (req.status === 'PENDING') {
                        const actionButton = document.createElement('button');
                        actionButton.textContent = 'Action';
                        actionButton.classList.add('btn', 'btn-primary', 'btn-sm');
                        actionButton.onclick = () => openActionModal(req);
                        actionsCell.appendChild(actionButton);
                    } else {
                        const viewButton = document.createElement('button');
                        viewButton.textContent = 'Details';
                        viewButton.classList.add('btn', 'btn-outline', 'btn-sm');
                        viewButton.onclick = () => openActionModal(req, true);
                        actionsCell.appendChild(viewButton);
                    }
                } catch (e) {
                    console.error("Error rendering row for request data:", req, e);
                    // Optionally, append a row indicating an error for this specific item
                    const errorRow = allLeavesTableBody.insertRow();
                    const cell = errorRow.insertCell();
                    cell.colSpan = 9; // Adjust to number of columns
                    cell.textContent = `Error rendering leave request ID: ${req.id || 'Unknown'}. Check console.`;
                    cell.style.color = 'red';
                }
            });
        } else {
            allLeavesTable.style.display = 'none'; // Hide table if no data
        }
    }

    function openActionModal(leaveRequest, readOnly = false) {
        clearMessage(leaveActionMessage);
        actionLeaveIdInput.value = leaveRequest.id;
        modalTitle.textContent = readOnly ? "Leave Request Details" : "Action Leave Request";
        modalEmployeeName.textContent = leaveRequest.employeeName || 'N/A';
        modalLeaveDates.textContent = `${formatDate(leaveRequest.startDate)} to ${formatDate(leaveRequest.endDate)}`;
        modalLeaveReason.textContent = leaveRequest.reason || 'N/A';
        adminRemarksInput.value = leaveRequest.adminRemarks || '';
        adminRemarksInput.readOnly = readOnly;

        approveLeaveBtn.style.display = readOnly ? 'none' : 'inline-block';
        rejectLeaveBtn.style.display = readOnly ? 'none' : 'inline-block';

        leaveActionModal.style.display = 'flex';
    }

    function closeActionModal() {
        leaveActionModal.style.display = 'none';
        adminRemarksInput.value = '';
    }

    cancelModalBtn.addEventListener('click', closeActionModal);
    leaveActionModal.addEventListener('click', function(event) {
        if (event.target === leaveActionModal) {
            closeActionModal();
        }
    });

    approveLeaveBtn.addEventListener('click', () => handleLeaveAction('APPROVED'));
    rejectLeaveBtn.addEventListener('click', () => handleLeaveAction('REJECTED'));

    async function handleLeaveAction(newStatus) {
        const leaveId = actionLeaveIdInput.value;
        const remarks = adminRemarksInput.value.trim();
        clearMessage(leaveActionMessage);
        approveLeaveBtn.disabled = true;
        rejectLeaveBtn.disabled = true;

        const actionData = { newStatus: newStatus, adminRemarks: remarks };
        const headers = { 'X-Admin-Id': loggedInUser.id };

        try {
            await makeApiCall(`/api/leaves/admin/${leaveId}/action`, 'PUT', actionData, headers, true, `Processing ${newStatus.toLowerCase()}...`);
            displayMessage(leaveActionMessage, `Leave request successfully ${newStatus.toLowerCase()}.`, 'success');
            setTimeout(() => {
                closeActionModal();
                fetchLeaveRequestsByStatus(currentFilterStatus);
            }, 1500);
        } catch (error) {
            console.error(`Error ${newStatus.toLowerCase()} leave:`, error);
            displayMessage(leaveActionMessage, error.message || `Failed to ${newStatus.toLowerCase()} leave request.`, 'error');
        } finally {
            approveLeaveBtn.disabled = false;
            rejectLeaveBtn.disabled = false;
        }
    }

    // Unified function to manage loading message and table visibility
    function showLoadingState(isLoading) {
        if (loadingAllLeavesMsg) {
            loadingAllLeavesMsg.style.display = isLoading ? 'block' : 'none';
        }
        if (allLeavesTable) {
            // Hide table when loading, visibility when not loading is handled by renderAllLeavesTable
            allLeavesTable.style.display = isLoading ? 'none' : (allLeavesTableBody.rows.length > 0 ? '' : 'none');
        }
    }

    // formatDate, displayMessage, clearMessage functions (ensure they are robust)
    function formatDate(dateStringOrArray, includeTime = false) {
        if (!dateStringOrArray) return 'N/A';
        try {
            let dateToFormat;
            if (Array.isArray(dateStringOrArray) && dateStringOrArray.length >= 3) {
                dateToFormat = new Date(dateStringOrArray[0], dateStringOrArray[1] - 1, dateStringOrArray[2],
                                        dateStringOrArray[3] || 0, dateStringOrArray[4] || 0, dateStringOrArray[5] || 0);
            } else if (typeof dateStringOrArray === 'string') {
                // More robust parsing for various string formats, especially date-only
                const dateOnlyStr = dateStringOrArray.split('T')[0];
                const parts = dateOnlyStr.split(/[-/]/); // Split by - or /
                if (parts.length === 3) {
                     dateToFormat = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
                } else {
                    dateToFormat = new Date(dateStringOrArray); // Fallback
                }
            } else { return String(dateStringOrArray); }

            if (isNaN(dateToFormat.getTime())) {
                 console.warn("formatDate created Invalid Date from:", dateStringOrArray);
                 return 'Invalid Date';
            }

            const options = { year: 'numeric', month: 'short', day: 'numeric' };
            if (includeTime) {
                options.hour = '2-digit';
                options.minute = '2-digit';
                // options.hour12 = true; // Optional
            }
            return dateToFormat.toLocaleDateString(undefined, options);
        } catch (e) {
            console.warn("Could not format date:", dateStringOrArray, e);
            return String(dateStringOrArray); // Fallback to original string
        }
    }

    function displayMessage(element, message, type = 'info') {
         if (element) {
            element.textContent = message;
            element.className = 'message-area'; // Reset base class
            if (type === 'error') element.classList.add('error-message');
            else if (type === 'success') element.classList.add('success-message');
            else element.classList.add('info-message');
            element.style.display = 'block';
            if (type === 'success' || type === 'info') { // Auto-hide for non-errors
                setTimeout(() => { clearMessage(element); }, 5000);
            }
        }
    }

    function clearMessage(element) {
        if (element) {
            element.textContent = '';
            element.style.display = 'none';
            element.className = 'message-area'; // Reset classes
        }
    }
});