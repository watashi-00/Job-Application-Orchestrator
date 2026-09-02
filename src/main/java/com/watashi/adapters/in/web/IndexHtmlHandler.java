package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class IndexHtmlHandler implements HttpHandler {

    private static final String HTML_CONTENT = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Job Application Orchestrator</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background-color: #f8fafc;
                    color: #0f172a;
                    font-size: 14px;
                    line-height: 1.5;
                    height: 100vh;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }
                header.navbar {
                    background-color: #0f172a;
                    color: #ffffff;
                    padding: 12px 20px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    border-bottom: 1px solid #1e293b;
                    flex-shrink: 0;
                }
                .navbar-brand {
                    font-size: 16px;
                    font-weight: 700;
                    letter-spacing: 0.05em;
                    display: flex;
                    align-items: center;
                    gap: 10px;
                }
                .navbar-tag {
                    font-size: 10px;
                    font-weight: 600;
                    background-color: #334155;
                    color: #94a3b8;
                    padding: 2px 6px;
                    border-radius: 3px;
                }
                .container {
                    display: grid;
                    grid-template-columns: 240px 1fr 320px;
                    gap: 16px;
                    padding: 16px;
                    flex: 1;
                    overflow: hidden;
                }
                .panel {
                    background-color: #ffffff;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }
                .panel-header {
                    padding: 12px 16px;
                    border-bottom: 1px solid #e2e8f0;
                    font-weight: 600;
                    color: #0f172a;
                    background-color: #ffffff;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    flex-shrink: 0;
                }
                .panel-content {
                    padding: 16px;
                    overflow-y: auto;
                    flex: 1;
                }
                .profile-title { font-size: 15px; font-weight: 700; color: #0f172a; margin-bottom: 4px; }
                .profile-sub { font-size: 12px; color: #64748b; margin-bottom: 12px; }
                .section-label { font-size: 11px; font-weight: 700; text-transform: uppercase; color: #64748b; margin: 12px 0 6px 0; letter-spacing: 0.05em; }
                .tag-list { display: flex; flex-wrap: wrap; gap: 4px; }
                .tag {
                    background-color: #f1f5f9;
                    color: #334155;
                    padding: 2px 8px;
                    border-radius: 4px;
                    font-size: 12px;
                    border: 1px solid #e2e8f0;
                }
                .tag-matched { background-color: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; font-weight: 600; }
                .tag-missing { background-color: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
                .tag-contract { background-color: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; font-weight: 600; }
                .tag-region { background-color: #f8fafc; color: #475569; border: 1px solid #cbd5e1; }

                .controls-bar {
                    display: flex;
                    flex-direction: column;
                    gap: 10px;
                    margin-bottom: 12px;
                    flex-shrink: 0;
                }
                .controls-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 12px;
                }
                .search-input {
                    width: 100%;
                    padding: 7px 12px;
                    font-size: 13px;
                    border: 1px solid #cbd5e1;
                    border-radius: 4px;
                    outline: none;
                    background-color: #ffffff;
                    color: #0f172a;
                    transition: border-color 0.15s ease;
                }
                .search-input:focus { border-color: #0f172a; }

                .filter-group { display: flex; gap: 4px; flex-wrap: wrap; }
                .btn-filter {
                    background-color: #ffffff;
                    border: 1px solid #e2e8f0;
                    color: #475569;
                    padding: 5px 10px;
                    font-size: 11px;
                    font-weight: 600;
                    border-radius: 4px;
                    cursor: pointer;
                }
                .btn-filter:hover { background-color: #f8fafc; color: #0f172a; }
                .btn-filter.active { background-color: #0f172a; color: #ffffff; border-color: #0f172a; }

                .btn-action {
                    background-color: #0f172a;
                    color: #ffffff;
                    border: 1px solid #0f172a;
                    padding: 6px 14px;
                    font-size: 12px;
                    font-weight: 600;
                    border-radius: 4px;
                    cursor: pointer;
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    white-space: nowrap;
                }
                .btn-action:hover { background-color: #1e293b; }
                .btn-action:disabled { opacity: 0.6; cursor: not-allowed; }

                .job-list { display: flex; flex-direction: column; gap: 8px; overflow-y: auto; flex: 1; }
                .job-card {
                    background-color: #ffffff;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    padding: 12px 14px;
                    cursor: pointer;
                    transition: border-color 0.15s ease;
                }
                .job-card:hover { border-color: #94a3b8; }
                .job-card.selected { border-color: #0f172a; background-color: #f8fafc; }
                .job-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 4px; }
                .job-card-title { font-weight: 600; font-size: 14px; color: #0f172a; }
                .job-card-company { font-size: 12px; color: #64748b; margin-bottom: 8px; }
                .job-card-meta { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; font-size: 11px; color: #475569; }

                .badge {
                    display: inline-flex;
                    align-items: center;
                    gap: 4px;
                    padding: 2px 8px;
                    border-radius: 4px;
                    font-size: 11px;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    flex-shrink: 0;
                }
                .badge-recommended { background-color: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
                .badge-conditional { background-color: #fffbeb; color: #d97706; border: 1px solid #fef08a; }
                .badge-rejected { background-color: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
                .badge-discovered { background-color: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; }
                .badge-applied { background-color: #e0f2fe; color: #0284c7; border: 1px solid #bae6fd; }
                .badge-ignored { background-color: #f3f4f6; color: #4b5563; border: 1px solid #e5e7eb; }

                .btn-card-action {
                    background-color: #ffffff;
                    border: 1px solid #cbd5e1;
                    color: #334155;
                    padding: 3px 8px;
                    font-size: 11px;
                    font-weight: 600;
                    border-radius: 4px;
                    cursor: pointer;
                    transition: all 0.15s ease;
                }
                .btn-card-action:hover { background-color: #f1f5f9; border-color: #94a3b8; }
                .btn-card-delete:hover { background-color: #fef2f2; color: #dc2626; border-color: #fecaca; }
                .job-checkbox { cursor: pointer; width: 15px; height: 15px; accent-color: #0f172a; }

                .detail-placeholder { color: #94a3b8; font-size: 13px; text-align: center; margin-top: 40px; }
                .detail-title { font-size: 16px; font-weight: 700; color: #0f172a; margin-bottom: 4px; }
                .detail-company { font-size: 13px; color: #64748b; margin-bottom: 12px; }
                .detail-meta-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
                .detail-meta-table td { padding: 4px 0; font-size: 12px; }
                .detail-meta-table td.label { color: #64748b; width: 90px; }
                .detail-meta-table td.val { font-weight: 500; color: #0f172a; }
                .detail-desc { font-size: 12px; color: #334155; line-height: 1.6; font-family: inherit; margin-top: 8px; max-height: 280px; overflow-y: auto; border: 1px solid #e2e8f0; padding: 10px; border-radius: 4px; background: #fafafa; white-space: pre-line; }
                .detail-link { display: inline-block; margin-top: 12px; color: #2563eb; text-decoration: none; font-size: 12px; font-weight: 600; }
                .detail-link:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <header class="navbar">
                <div class="navbar-brand">
                    <span>JOB APPLICATION ORCHESTRATOR</span>
                    <span class="navbar-tag">DASHBOARD</span>
                </div>
                <div id="status-indicator" style="font-size: 12px; color: #94a3b8;">Dashboard Live</div>
            </header>

            <div class="container">
                <!-- Column 1: Candidate Profile Sidebar (240px) -->
                <div class="panel">
                    <div class="panel-header">
                        <span>Candidate Profile</span>
                    </div>
                    <div class="panel-content" id="profile-panel">
                        <div class="profile-sub">Loading profile...</div>
                    </div>
                    <div style="padding: 0 16px 16px 16px;">
                        <input type="file" id="pdf-file-input" accept=".pdf" style="display:none;" onchange="uploadPdfFile(this)" />
                        <button id="btn-upload-pdf" class="btn-action" style="width: 100%; margin-top: 12px; justify-content: center;" onclick="document.getElementById('pdf-file-input').click()"><span>📄 Upload Resume PDF</span></button>
                    </div>
                </div>

                <!-- Column 2: Job Feed & Controls (1fr) -->
                <div class="panel" style="padding: 16px;">
                    <div class="controls-bar">
                        <!-- Dynamic Tag Pills Bar -->
                        <div id="tag-pills-bar" style="display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-bottom: 4px;"></div>

                        <!-- Search Bar -->
                        <input id="search-input" class="search-input" type="text" placeholder="🔍 Search title, company, skills, or description..." oninput="renderJobs()" />

                        <div class="controls-row">
                            <div class="filter-group">
                                <button class="btn-filter active" onclick="setStatusFilter('ALL', this)">ALL</button>
                                <button class="btn-filter" onclick="setStatusFilter('RECOMMENDED', this)">RECOMMENDED</button>
                                <button class="btn-filter" onclick="setStatusFilter('CONDITIONAL', this)">CONDITIONAL</button>
                                <button class="btn-filter" onclick="setStatusFilter('REJECTED', this)">REJECTED</button>
                                <button class="btn-filter" onclick="setStatusFilter('APPLIED', this)">APPLIED</button>
                                <button class="btn-filter" onclick="setStatusFilter('IGNORED', this)">IGNORED</button>
                            </div>
                            <div style="display: flex; align-items: center; gap: 12px;">
                                <span id="job-count" style="font-size: 12px; color: #64748b; font-weight: 600;">0 Jobs</span>
                                <button id="btn-discover" class="btn-action" onclick="discoverJobs()">
                                    <span>Discover Jobs</span>
                                </button>
                            </div>
                        </div>

                        <!-- Region / Location Filter Row -->
                        <div class="controls-row" style="margin-top: 4px;">
                            <div class="filter-group">
                                <span style="font-size: 11px; color: #64748b; font-weight: 600; display: flex; align-items: center; margin-right: 4px;">REGION:</span>
                                <button class="btn-filter active" onclick="setRegionFilter('ALL', this)">ALL REGIONS</button>
                                <button class="btn-filter" onclick="setRegionFilter('WORLDWIDE', this)">WORLDWIDE</button>
                                <button class="btn-filter" onclick="setRegionFilter('AMERICAS', this)">AMERICAS / LATAM</button>
                                <button class="btn-filter" onclick="setRegionFilter('USA', this)">USA / EUROPE</button>
                            </div>
                        </div>
                    </div>

                    <!-- Batch Mass Action Bar -->
                    <div id="batch-action-bar" style="display: none; background-color: #f1f5f9; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 12px; margin-bottom: 12px; align-items: center; justify-content: space-between; flex-shrink: 0;">
                        <div style="font-size: 12px; font-weight: 600; color: #334155;">
                            <span id="selected-count">0</span> jobs selected
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <button class="btn-action" style="background-color: #0284c7; border-color: #0284c7;" onclick="batchApplySelected()">🚀 Apply Selected</button>
                            <button class="btn-action" style="background-color: #475569; border-color: #475569;" onclick="batchIgnoreSelected()">🚫 Ignore Selected</button>
                            <button class="btn-action" style="background-color: #dc2626; border-color: #dc2626;" onclick="batchDeleteSelected()">🗑️ Delete Selected</button>
                        </div>
                    </div>

                    <div class="job-list" id="job-list">
                        <div style="text-align: center; color: #94a3b8; padding: 40px;">Loading job opportunities...</div>
                    </div>
                </div>

                <!-- Column 3: Job Details, Recruiter Inbox & Submission Audit Log (320px) -->
                <div style="display: flex; flex-direction: column; gap: 16px; height: 100%; overflow: hidden;">
                    <div class="panel" style="flex: 1; min-height: 0;">
                        <div class="panel-header">
                            <span>Job Details</span>
                        </div>
                        <div class="panel-content" id="detail-panel">
                            <div class="detail-placeholder">Select a job card to view full details</div>
                        </div>
                    </div>
                    <!-- Recruiter Inbox Sync Panel -->
                    <div class="panel" id="inbox-panel" style="height: 180px; flex-shrink: 0;">
                        <div class="panel-header">
                            <span>Recruiter Inbox (Port 2525)</span>
                            <div style="display: flex; gap: 4px;">
                                <button class="btn-filter" style="font-size: 10px; padding: 2px 6px;" onclick="testSimulateEmail()">📬 Test Email</button>
                                <button class="btn-filter" style="font-size: 10px; padding: 2px 6px;" onclick="loadInbox()">🔄</button>
                            </div>
                        </div>
                        <div class="panel-content" id="inbox-content" style="padding: 10px; font-size: 11px;">
                            <div style="color: #94a3b8; text-align: center; padding: 20px;">No recruiter emails received yet.</div>
                        </div>
                    </div>
                    <div class="panel" style="height: 180px; flex-shrink: 0;">
                        <div class="panel-header">
                            <span>Submission Audit Log</span>
                            <button class="btn-filter" style="font-size: 10px; padding: 2px 6px;" onclick="fetchDispatchLogs()">🔄 Refresh</button>
                        </div>
                        <div class="panel-content" id="dispatch-log-panel" style="padding: 10px; font-size: 11px;">
                            <div style="color: #94a3b8; text-align: center; padding: 20px;">No application dispatches logged yet.</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Credentials Vault Modal -->
            <div id="credentials-modal" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(15, 23, 42, 0.6); z-index: 1000; align-items: center; justify-content: center;">
                <div style="background-color: #ffffff; border-radius: 8px; width: 400px; padding: 20px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);">
                    <div style="font-size: 16px; font-weight: 700; margin-bottom: 8px; color: #0f172a;">🔑 Resolve Platform Credentials</div>
                    <div style="font-size: 12px; color: #64748b; margin-bottom: 16px;">
                        Authentication required for <strong id="modal-domain-name">domain</strong>. Save credentials/token to automate future dispatches.
                    </div>
                    <input type="hidden" id="modal-domain-input" />
                    <div style="margin-bottom: 12px;">
                        <label style="display: block; font-size: 11px; font-weight: 700; color: #475569; margin-bottom: 4px;">USERNAME / EMAIL</label>
                        <input id="modal-username-input" class="search-input" type="text" placeholder="user@example.com" />
                    </div>
                    <div style="margin-bottom: 16px;">
                        <label style="display: block; font-size: 11px; font-weight: 700; color: #475569; margin-bottom: 4px;">API TOKEN / SESSION COOKIE</label>
                        <input id="modal-token-input" class="search-input" type="password" placeholder="Session cookie or Auth token" />
                    </div>
                    <div style="display: flex; justify-content: flex-end; gap: 8px;">
                        <button class="btn-filter" onclick="closeCredentialsModal()">Cancel</button>
                        <button class="btn-action" onclick="submitCredentialsModal()">Save & Proceed</button>
                    </div>
                </div>
            </div>

            <script>
                let rawEvaluatedItems = [];
                let activeStatusFilter = 'ALL';
                let activeRegionFilter = 'ALL';
                let activeTagFilter = null;
                let customTags = [];
                let selectedJobId = null;
                let selectedJobIds = new Set();

                document.addEventListener('DOMContentLoaded', () => {
                    loadProfile();
                    loadJobs();
                    fetchDispatchLogs();
                    loadCustomTags();
                    loadInbox();
                });

                async function loadProfile() {
                    const panel = document.getElementById('profile-panel');
                    try {
                        const res = await fetch('/api/profile');
                        if (!res.ok) {
                            panel.innerHTML = '<div class="profile-sub">No candidate profile loaded</div>';
                            return;
                        }
                        const profile = await res.json();
                        const salary = profile.desiredSalary ? `${profile.desiredSalary.currency || ''} ${profile.desiredSalary.minAmount || profile.desiredSalary.min || ''}-${profile.desiredSalary.maxAmount || profile.desiredSalary.max || ''}` : 'Not specified';

                        panel.innerHTML = `
                            <div class="profile-title">${escapeHtml(profile.title || 'Candidate Profile')}</div>
                            <div class="profile-sub">${escapeHtml(profile.summary || '')}</div>

                            <div class="section-label">Target Seniority</div>
                            <div class="tag-list">
                                ${(profile.targetSeniorities || []).map(s => `<span class="tag">${escapeHtml(s)}</span>`).join('') || '<span class="profile-sub">None</span>'}
                            </div>

                            <div class="section-label">Preferred Work Mode</div>
                            <div class="tag-list">
                                ${(profile.preferredWorkModes || []).map(w => `<span class="tag">${escapeHtml(w)}</span>`).join('') || '<span class="profile-sub">None</span>'}
                            </div>

                            <div class="section-label">Extracted Skills</div>
                            <div class="tag-list">
                                ${(profile.skills || []).map(s => `<span class="tag">${escapeHtml(typeof s === 'string' ? s : s.name)}</span>`).join('') || '<span class="profile-sub">None</span>'}
                            </div>

                            <div class="section-label">Desired Salary</div>
                            <div style="font-size: 12px; color: #334155;">${escapeHtml(salary)}</div>
                        `;
                    } catch (err) {
                        panel.innerHTML = '<div class="profile-sub">Error loading profile</div>';
                    }
                }

                async function loadJobs() {
                    const listEl = document.getElementById('job-list');
                    try {
                        const res = await fetch('/api/jobs');
                        if (!res.ok) throw new Error('Failed to fetch jobs');
                        rawEvaluatedItems = await res.json();
                        renderJobs();
                    } catch (err) {
                        listEl.innerHTML = `<div style="text-align: center; color: #dc2626; padding: 40px;">Error loading jobs: ${escapeHtml(err.message)}</div>`;
                    }
                }

                function getEffectiveStatus(item) {
                    const job = item.job || item;
                    const match = item.match || null;
                    if (job.status && job.status !== 'DISCOVERED' && job.status !== 'EVALUATED') {
                        return job.status;
                    }
                    return match ? match.status : (job.status || 'DISCOVERED');
                }

                function renderJobs() {
                    const listEl = document.getElementById('job-list');
                    const countEl = document.getElementById('job-count');
                    const searchQuery = (document.getElementById('search-input').value || '').toLowerCase().trim();

                    const filtered = rawEvaluatedItems.filter(item => {
                        const job = item.job || item;
                        const st = getEffectiveStatus(item);

                        if (st === 'DELETED' && activeStatusFilter !== 'DELETED') {
                            return false;
                        }

                        // Status Filter
                        if (activeStatusFilter !== 'ALL' && st.toUpperCase() !== activeStatusFilter) {
                            return false;
                        }

                        // Region Filter
                        const loc = (job.location || '').toUpperCase();
                        if (activeRegionFilter === 'WORLDWIDE' && !loc.includes('WORLDWIDE')) return false;
                        if (activeRegionFilter === 'AMERICAS' && !(loc.includes('AMERICA') || loc.includes('LATAM') || loc.includes('US'))) return false;
                        if (activeRegionFilter === 'USA' && !(loc.includes('USA') || loc.includes('US') || loc.includes('EUROPE'))) return false;

                        // Custom Tag Filter
                        if (activeTagFilter) {
                            const tagLower = activeTagFilter.toLowerCase();
                            const title = (job.title || '').toLowerCase();
                            const company = (job.company || '').toLowerCase();
                            const desc = (job.description || '').toLowerCase();
                            const skills = (job.requiredSkills || []).map(s => typeof s === 'string' ? s : s.name).join(' ').toLowerCase();

                            if (!title.includes(tagLower) && !company.includes(tagLower) && !desc.includes(tagLower) && !skills.includes(tagLower)) {
                                return false;
                            }
                        }

                        // Keyword Search Filter
                        if (searchQuery.length > 0) {
                            const title = (job.title || '').toLowerCase();
                            const company = (job.company || '').toLowerCase();
                            const desc = (job.description || '').toLowerCase();
                            const skills = (job.requiredSkills || []).map(s => typeof s === 'string' ? s : s.name).join(' ').toLowerCase();

                            return title.includes(searchQuery) || company.includes(searchQuery) || desc.includes(searchQuery) || skills.includes(searchQuery);
                        }

                        return true;
                    });

                    countEl.textContent = `${filtered.length} Jobs`;

                    if (filtered.length === 0) {
                        listEl.innerHTML = '<div style="text-align: center; color: #94a3b8; padding: 40px;">No jobs found matching criteria.</div>';
                        return;
                    }

                    listEl.innerHTML = filtered.map(item => {
                        const job = item.job || item;
                        const match = item.match || null;
                        const st = getEffectiveStatus(item);
                        const score = match ? match.overallScore.toFixed(1) : null;

                        let badgeClass = 'badge-discovered';
                        let badgeSymbol = '⚪';
                        if (st === 'RECOMMENDED') { badgeClass = 'badge-recommended'; badgeSymbol = '🟢'; }
                        else if (st === 'CONDITIONAL') { badgeClass = 'badge-conditional'; badgeSymbol = '🟡'; }
                        else if (st === 'REJECTED') { badgeClass = 'badge-rejected'; badgeSymbol = '🔴'; }
                        else if (st === 'APPLIED') { badgeClass = 'badge-applied'; badgeSymbol = '🚀'; }
                        else if (st === 'IGNORED') { badgeClass = 'badge-ignored'; badgeSymbol = '🚫'; }

                        const isSelected = job.id === selectedJobId;
                        const isChecked = selectedJobIds.has(job.id);
                        const isContract = (job.title || '').toLowerCase().includes('contract') || (job.title || '').toLowerCase().includes('freelance');
                        const jobTypeLabel = isContract ? 'CONTRACT / FREELANCE' : 'FULL-TIME / PJ';

                        return `
                            <div class="job-card ${isSelected ? 'selected' : ''}" onclick="selectJob('${escapeHtml(job.id)}')">
                                <div class="job-card-header">
                                    <div style="display: flex; align-items: center; gap: 8px;">
                                        <input type="checkbox" class="job-checkbox" value="${escapeHtml(job.id)}" ${isChecked ? 'checked' : ''} onclick="event.stopPropagation(); toggleJobSelection('${escapeHtml(job.id)}', this.checked)" />
                                        <span class="job-card-title">${escapeHtml(job.title || 'Untitled')}</span>
                                    </div>
                                    <span class="badge ${badgeClass}">${badgeSymbol} ${escapeHtml(st)} ${score ? `(${score}%)` : ''}</span>
                                </div>
                                <div class="job-card-company">${escapeHtml(job.company || 'Unknown')}</div>
                                <div class="job-card-meta" style="justify-content: space-between;">
                                    <div style="display: flex; flex-wrap: wrap; gap: 6px; align-items: center;">
                                        <span class="tag tag-region">📍 ${escapeHtml(job.location || 'Remote')}</span>
                                        <span class="tag">💼 ${escapeHtml(job.seniorityLevel || 'N/A')}</span>
                                        <span class="tag tag-contract">📄 ${jobTypeLabel}</span>
                                    </div>
                                    <div style="display: flex; gap: 4px;" onclick="event.stopPropagation();">
                                        <button class="btn-card-action" title="Apply" onclick="applyToJob('${escapeHtml(job.id)}')">🚀 Apply</button>
                                        <button class="btn-card-action" title="Ignore" onclick="ignoreJob('${escapeHtml(job.id)}')">🚫 Ignore</button>
                                        <button class="btn-card-action btn-card-delete" title="Delete" onclick="deleteJob('${escapeHtml(job.id)}')">🗑️ Delete</button>
                                    </div>
                                </div>
                            </div>
                        `;
                    }).join('');
                }

                function selectJob(id) {
                    selectedJobId = id;
                    renderJobs();
                    const item = rawEvaluatedItems.find(i => (i.job ? i.job.id : i.id) === id);
                    const detailEl = document.getElementById('detail-panel');
                    if (!item) {
                        detailEl.innerHTML = '<div class="detail-placeholder">Select a job card to view full details</div>';
                        return;
                    }

                    const job = item.job || item;
                    const match = item.match || null;

                    const reqSkills = (job.requiredSkills || []).map(s => typeof s === 'string' ? s : s.name).join(', ') || 'None listed';
                    const optSkills = (job.optionalSkills || []).map(s => typeof s === 'string' ? s : s.name).join(', ') || 'None listed';
                    const matchedSkills = match && match.matchedSkills ? match.matchedSkills.map(s => s.name).join(', ') : 'None';
                    const missingSkills = match && match.missingRequiredSkills ? match.missingRequiredSkills.map(s => s.name).join(', ') : 'None';
                    const conflicts = match && match.conflicts ? match.conflicts.join('; ') : 'None';
                    const st = getEffectiveStatus(item);

                    detailEl.innerHTML = `
                        <div class="detail-title">${escapeHtml(job.title || 'Untitled')}</div>
                        <div class="detail-company">${escapeHtml(job.company || 'Unknown')}</div>

                        <table class="detail-meta-table">
                            <tr><td class="label">Status</td><td class="val">${escapeHtml(st)} ${match ? `(${match.overallScore.toFixed(1)}%)` : ''}</td></tr>
                            <tr><td class="label">Seniority</td><td class="val">${escapeHtml(job.seniorityLevel || 'N/A')}</td></tr>
                            <tr><td class="label">Work Mode</td><td class="val">${escapeHtml(job.workMode || 'N/A')}</td></tr>
                            <tr><td class="label">Location</td><td class="val">${escapeHtml(job.location || 'N/A')}</td></tr>
                            <tr><td class="label">Matched</td><td class="val" style="color: #16a34a; font-weight: 600;">${escapeHtml(matchedSkills)}</td></tr>
                            <tr><td class="label">Missing</td><td class="val" style="color: #dc2626;">${escapeHtml(missingSkills)}</td></tr>
                            ${conflicts !== 'None' ? `<tr><td class="label">Conflicts</td><td class="val" style="color: #d97706;">${escapeHtml(conflicts)}</td></tr>` : ''}
                        </table>

                        <div class="section-label">Job Description</div>
                        <div class="detail-desc">${escapeHtml(job.description || 'No description available.')}</div>

                        ${job.sourceUrl ? `<a class="detail-link" href="${escapeHtml(job.sourceUrl)}" target="_blank">View Original Post ↗</a>` : ''}

                        <button id="btn-generate-coverletter" class="btn-action" style="width: 100%; margin-top: 12px; justify-content: center;" onclick="generateCoverLetter('${escapeHtml(job.id)}')">
                            <span>📝 Generate Cover Letter</span>
                        </button>

                        <div id="coverletter-container" style="display: none; margin-top: 12px; border-top: 1px solid #e2e8f0; padding-top: 12px;">
                            <div class="section-label">Cover Letter</div>
                            <textarea id="coverletter-text" style="width: 100%; height: 160px; font-family: inherit; font-size: 12px; padding: 8px; border: 1px solid #cbd5e1; border-radius: 4px; resize: vertical; margin-bottom: 8px;" readonly></textarea>
                            <button id="btn-copy-coverletter" class="btn-action" style="width: 100%; justify-content: center;" onclick="copyCoverLetter()">
                                <span>📋 Copy Cover Letter</span>
                            </button>
                        </div>
                    `;
                }

                function toggleJobSelection(id, isChecked) {
                    if (isChecked) {
                        selectedJobIds.add(id);
                    } else {
                        selectedJobIds.delete(id);
                    }
                    updateBatchBar();
                }

                function updateBatchBar() {
                    const bar = document.getElementById('batch-action-bar');
                    const countEl = document.getElementById('selected-count');
                    if (!bar || !countEl) return;
                    if (selectedJobIds.size > 0) {
                        bar.style.display = 'flex';
                        countEl.textContent = selectedJobIds.size;
                    } else {
                        bar.style.display = 'none';
                    }
                }

                async function applyToJob(id) {
                    await dispatchApplication(id);
                }

                async function ignoreJob(id) {
                    try {
                        const res = await fetch('/api/jobs/status', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ jobId: id, status: 'IGNORED' })
                        });
                        if (!res.ok) throw new Error('Status update failed');
                        await loadJobs();
                        if (selectedJobId === id) selectJob(id);
                    } catch (err) {
                        alert('Failed to ignore job: ' + err.message);
                    }
                }

                async function deleteJob(id) {
                    try {
                        const res = await fetch('/api/jobs/status', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ jobId: id, status: 'DELETED' })
                        });
                        if (!res.ok) throw new Error('Status update failed');
                        selectedJobIds.delete(id);
                        updateBatchBar();
                        await loadJobs();
                        if (selectedJobId === id) selectJob(id);
                    } catch (err) {
                        alert('Failed to delete job: ' + err.message);
                    }
                }

                async function batchApplySelected() {
                    if (selectedJobIds.size === 0) return;
                    try {
                        const ids = Array.from(selectedJobIds);
                        for (const id of ids) {
                            await dispatchApplication(id);
                        }
                        selectedJobIds.clear();
                        updateBatchBar();
                        await loadJobs();
                        if (selectedJobId) selectJob(selectedJobId);
                    } catch (err) {
                        alert('Failed to apply selected jobs: ' + err.message);
                    }
                }

                async function dispatchApplication(jobId) {
                    try {
                        const res = await fetch('/api/applications/dispatch', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ jobId: jobId, sendCoverLetter: false })
                        });
                        if (!res.ok) throw new Error('Dispatch failed with status ' + res.status);
                        const log = await res.json();
                        await fetchDispatchLogs();
                        await loadJobs();
                        if (selectedJobId === jobId) selectJob(jobId);
                        if (log.requiresHumanAssistance) {
                            openCredentialsModal(log.domain || '');
                        }
                        return log;
                    } catch (err) {
                        alert('Failed to dispatch application: ' + err.message);
                    }
                }

                async function fetchDispatchLogs() {
                    const logPanel = document.getElementById('dispatch-log-panel');
                    if (!logPanel) return;
                    try {
                        const res = await fetch('/api/applications/logs');
                        if (!res.ok) return;
                        const logs = await res.json();
                        if (!logs || logs.length === 0) {
                            logPanel.innerHTML = '<div style="color: #94a3b8; text-align: center; padding: 20px;">No application dispatches logged yet.</div>';
                            return;
                        }
                        logPanel.innerHTML = logs.slice().reverse().map(log => {
                            let indicator = '🟢';
                            let statusColor = '#16a34a';
                            if (log.requiresHumanAssistance || log.status === 'NEEDS_HUMAN_ASSISTANCE') {
                                indicator = '⚠️';
                                statusColor = '#d97706';
                            } else if (log.status === 'SUBMITTING') {
                                indicator = '🟡';
                                statusColor = '#ca8a04';
                            }
                            const timeStr = log.timestamp ? (log.timestamp.includes('T') ? log.timestamp.split('T')[1].substring(0, 8) : log.timestamp) : '';
                            return `
                                <div style="border-bottom: 1px solid #f1f5f9; padding: 6px 0;">
                                    <div style="display: flex; justify-content: space-between; align-items: center;">
                                        <span style="font-weight: 600; color: ${statusColor};">${indicator} ${escapeHtml(log.jobTitle || log.jobId)}</span>
                                        <span style="font-size: 10px; color: #94a3b8;">${escapeHtml(timeStr)}</span>
                                    </div>
                                    <div style="font-size: 10px; color: #64748b;">${escapeHtml(log.company)} (${escapeHtml(log.domain)})</div>
                                    <div style="font-size: 10px; color: #334155; margin-top: 2px;">${escapeHtml(log.message)}</div>
                                    ${log.requiresHumanAssistance ? `<button class="btn-card-action" style="margin-top: 4px; background: #fffbeb; color: #b45309; border-color: #fde68a;" onclick="openCredentialsModal('${escapeHtml(log.domain)}')">🔑 Resolve Login / Save Session</button>` : ''}
                                </div>
                            `;
                        }).join('');
                    } catch (err) {
                        logPanel.innerHTML = '<div style="color: #dc2626; text-align: center; padding: 10px;">Error loading audit logs</div>';
                    }
                }

                function openCredentialsModal(domain) {
                    const modal = document.getElementById('credentials-modal');
                    const domainName = document.getElementById('modal-domain-name');
                    const domainInput = document.getElementById('modal-domain-input');
                    const usernameInput = document.getElementById('modal-username-input');
                    const tokenInput = document.getElementById('modal-token-input');
                    if (!modal) return;
                    if (domainName) domainName.textContent = domain || 'target domain';
                    if (domainInput) domainInput.value = domain || '';
                    if (usernameInput) usernameInput.value = '';
                    if (tokenInput) tokenInput.value = '';
                    modal.style.display = 'flex';
                }

                function closeCredentialsModal() {
                    const modal = document.getElementById('credentials-modal');
                    if (modal) modal.style.display = 'none';
                }

                async function submitCredentialsModal() {
                    const domain = (document.getElementById('modal-domain-input').value || '').trim();
                    const username = (document.getElementById('modal-username-input').value || '').trim();
                    const token = (document.getElementById('modal-token-input').value || '').trim();
                    if (!domain || !username || !token) {
                        alert('Please fill in all fields.');
                        return;
                    }
                    try {
                        const res = await fetch('/api/credentials', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ domain, username, token })
                        });
                        if (!res.ok) throw new Error('Failed to save credentials');
                        closeCredentialsModal();
                        alert('Credentials saved for ' + domain + '!');
                    } catch (err) {
                        alert('Error saving credentials: ' + err.message);
                    }
                }

                async function batchIgnoreSelected() {
                    if (selectedJobIds.size === 0) return;
                    try {
                        const ids = Array.from(selectedJobIds);
                        const res = await fetch('/api/jobs/batch-status', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ jobIds: ids, status: 'IGNORED' })
                        });
                        if (!res.ok) throw new Error('Batch status update failed');
                        selectedJobIds.clear();
                        updateBatchBar();
                        await loadJobs();
                        if (selectedJobId) selectJob(selectedJobId);
                    } catch (err) {
                        alert('Failed to ignore selected jobs: ' + err.message);
                    }
                }

                async function batchDeleteSelected() {
                    if (selectedJobIds.size === 0) return;
                    try {
                        const ids = Array.from(selectedJobIds);
                        const res = await fetch('/api/jobs/batch-status', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ jobIds: ids, status: 'DELETED' })
                        });
                        if (!res.ok) throw new Error('Batch status update failed');
                        selectedJobIds.clear();
                        updateBatchBar();
                        await loadJobs();
                        if (selectedJobId) selectJob(selectedJobId);
                    } catch (err) {
                        alert('Failed to delete selected jobs: ' + err.message);
                    }
                }

                async function generateCoverLetter(jobId) {
                    const btn = document.getElementById('btn-generate-coverletter');
                    const container = document.getElementById('coverletter-container');
                    const textarea = document.getElementById('coverletter-text');
                    if (!btn) return;
                    const originalText = btn.innerHTML;
                    btn.disabled = true;
                    btn.innerHTML = '<span>Generating...</span>';
                    try {
                        const res = await fetch('/api/cover-letter?jobId=' + encodeURIComponent(jobId));
                        if (!res.ok) {
                            throw new Error('Failed to generate cover letter (status ' + res.status + ')');
                        }
                        const data = await res.json();
                        textarea.value = data.coverLetter || '';
                        container.style.display = 'block';
                    } catch (err) {
                        alert('Error generating cover letter: ' + err.message);
                    } finally {
                        btn.disabled = false;
                        btn.innerHTML = originalText;
                    }
                }

                async function copyCoverLetter() {
                    const textarea = document.getElementById('coverletter-text');
                    const btn = document.getElementById('btn-copy-coverletter');
                    if (!textarea || !textarea.value) return;
                    try {
                        await navigator.clipboard.writeText(textarea.value);
                        if (btn) {
                            const originalText = btn.innerHTML;
                            btn.innerHTML = '<span>✅ Copied!</span>';
                            setTimeout(() => { btn.innerHTML = originalText; }, 2000);
                        }
                    } catch (err) {
                        alert('Failed to copy text: ' + err.message);
                    }
                }

                function setStatusFilter(filter, btn) {
                    activeStatusFilter = filter;
                    btn.parentElement.querySelectorAll('.btn-filter').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    renderJobs();
                }

                function setRegionFilter(region, btn) {
                    activeRegionFilter = region;
                    btn.parentElement.querySelectorAll('.btn-filter').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    renderJobs();
                }

                async function discoverJobs() {
                    const btn = document.getElementById('btn-discover');
                    btn.disabled = true;
                    btn.innerHTML = '<span>Discovering...</span>';
                    try {
                        await fetch('/api/jobs', { method: 'POST' });
                        await loadJobs();
                        await loadProfile();
                    } catch (err) {
                        alert('Failed to discover jobs: ' + err.message);
                    } finally {
                        btn.disabled = false;
                        btn.innerHTML = '<span>Discover Jobs</span>';
                    }
                }

                async function uploadPdfFile(input) {
                    if (!input.files || input.files.length === 0) return;
                    const file = input.files[0];
                    const btn = document.getElementById('btn-upload-pdf');
                    const originalText = btn ? btn.innerHTML : '';
                    if (btn) {
                        btn.disabled = true;
                        btn.innerHTML = '<span>Uploading...</span>';
                    }
                    try {
                        const buffer = await file.arrayBuffer();
                        const res = await fetch('/api/profile', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/pdf' },
                            body: buffer
                        });
                        if (!res.ok) {
                            throw new Error('Upload failed with status ' + res.status);
                        }
                        await loadProfile();
                        await loadJobs();
                    } catch (err) {
                        alert('Failed to upload PDF: ' + err.message);
                    } finally {
                        if (btn) {
                            btn.disabled = false;
                            btn.innerHTML = originalText;
                        }
                        input.value = '';
                    }
                }

                async function loadCustomTags() {
                    const bar = document.getElementById('tag-pills-bar');
                    if (!bar) return;
                    try {
                        const res = await fetch('/api/tags');
                        if (!res.ok) return;
                        customTags = await res.json();
                        renderTagPills();
                    } catch (err) {
                        console.error('Error loading custom tags:', err);
                    }
                }

                function renderTagPills() {
                    const bar = document.getElementById('tag-pills-bar');
                    if (!bar) return;
                    if (!customTags || customTags.length === 0) {
                        bar.innerHTML = '<span style="font-size: 11px; color: #64748b; font-weight: 600; margin-right: 4px;">TAGS:</span><button class="btn-filter" style="border-style: dashed;" onclick="createCustomTag()">+ Add Tag</button>';
                        return;
                    }
                    let html = '<span style="font-size: 11px; color: #64748b; font-weight: 600; display: flex; align-items: center; margin-right: 4px;">TAGS:</span>';
                    html += customTags.map(t => {
                        const isActive = activeTagFilter === t.name;
                        const color = t.colorHex || '#6c757d';
                        return `<button class="btn-filter ${isActive ? 'active' : ''}" style="${isActive ? '' : 'border-color:' + color + '; color:' + color + ';'}" onclick="toggleTagFilter('${escapeHtml(t.name)}', this)">${escapeHtml(t.name)}</button>`;
                    }).join('');
                    html += `<button class="btn-filter" style="border-style: dashed;" onclick="createCustomTag()">+ Add Tag</button>`;
                    bar.innerHTML = html;
                }

                function toggleTagFilter(name, el) {
                    if (activeTagFilter === name) {
                        activeTagFilter = null;
                    } else {
                        activeTagFilter = name;
                    }
                    renderTagPills();
                    renderJobs();
                }

                async function createCustomTag() {
                    const name = prompt('Enter custom tag name:');
                    if (!name || !name.trim()) return;
                    try {
                        const res = await fetch('/api/tags', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ name: name.trim() })
                        });
                        if (!res.ok) throw new Error('Failed to create tag');
                        await loadCustomTags();
                    } catch (err) {
                        alert('Error creating tag: ' + err.message);
                    }
                }

                async function loadInbox() {
                    const content = document.getElementById('inbox-content');
                    if (!content) return;
                    try {
                        const res = await fetch('/api/inbox');
                        if (!res.ok) return;
                        const emails = await res.json();
                        if (!emails || emails.length === 0) {
                            content.innerHTML = '<div style="color: #94a3b8; text-align: center; padding: 20px;">No recruiter emails received yet.</div>';
                            return;
                        }
                        content.innerHTML = emails.slice().reverse().map(email => {
                            const statusBadge = email.detectedStatus ? `<span class="badge badge-recommended" style="font-size: 9px; padding: 1px 4px;">${escapeHtml(email.detectedStatus)}</span>` : '';
                            const matchInfo = email.matchedCompany ? `<span style="color: #0284c7; font-weight: 600;">Matched: ${escapeHtml(email.matchedCompany)}</span>` : '';
                            return `
                                <div style="border-bottom: 1px solid #f1f5f9; padding: 6px 0;">
                                    <div style="display: flex; justify-content: space-between; align-items: center;">
                                        <span style="font-weight: 600; color: #0f172a;">📩 ${escapeHtml(email.sender || 'Unknown')}</span>
                                        ${statusBadge}
                                    </div>
                                    <div style="font-size: 11px; font-weight: 600; color: #334155; margin-top: 2px;">${escapeHtml(email.subject || '(No Subject)')}</div>
                                    <div style="font-size: 10px; color: #64748b; margin-top: 2px;">${escapeHtml((email.bodyText || '').substring(0, 80))}${email.bodyText && email.bodyText.length > 80 ? '...' : ''}</div>
                                    ${matchInfo ? `<div style="font-size: 10px; margin-top: 2px;">${matchInfo}</div>` : ''}
                                </div>
                            `;
                        }).join('');
                    } catch (err) {
                        content.innerHTML = '<div style="color: #dc2626; text-align: center; padding: 10px;">Error loading inbox</div>';
                    }
                }

                async function testSimulateEmail() {
                    try {
                        const res = await fetch('/api/inbox/parse', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                sender: 'recruiter@techcorp.io',
                                recipient: 'candidate@watashi.com',
                                subject: 'Interview Invitation for Senior Dev Role',
                                bodyText: 'Hello, we were impressed by your profile at TechCorp and would like to schedule an interview.'
                            })
                        });
                        if (!res.ok) throw new Error('Failed to simulate email');
                        await loadInbox();
                        await loadJobs();
                    } catch (err) {
                        alert('Error simulating email: ' + err.message);
                    }
                }

                function escapeHtml(str) {
                    if (!str) return '';
                    return String(str)
                        .replace(/&/g, '&amp;')
                        .replace(/</g, '&lt;')
                        .replace(/>/g, '&gt;')
                        .replace(/"/g, '&quot;')
                        .replace(/'/g, '&#039;');
                }
            </script>
        </body>
        </html>
        """;

    public IndexHtmlHandler() {}

    public IndexHtmlHandler(
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository) {}

    public IndexHtmlHandler(
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase) {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        byte[] responseBytes = HTML_CONTENT.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
