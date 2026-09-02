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
                .controls-bar {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 12px;
                    margin-bottom: 12px;
                    flex-shrink: 0;
                }
                .filter-group { display: flex; gap: 4px; }
                .btn-filter {
                    background-color: #ffffff;
                    border: 1px solid #e2e8f0;
                    color: #475569;
                    padding: 6px 12px;
                    font-size: 12px;
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
                .job-card-meta { display: flex; gap: 12px; font-size: 12px; color: #475569; }
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
                .detail-placeholder { color: #94a3b8; font-size: 13px; text-align: center; margin-top: 40px; }
                .detail-title { font-size: 16px; font-weight: 700; color: #0f172a; margin-bottom: 4px; }
                .detail-company { font-size: 13px; color: #64748b; margin-bottom: 12px; }
                .detail-meta-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
                .detail-meta-table td { padding: 4px 0; font-size: 12px; }
                .detail-meta-table td.label { color: #64748b; width: 90px; }
                .detail-meta-table td.val { font-weight: 500; color: #0f172a; }
                .detail-desc { font-size: 12px; color: #334155; line-height: 1.6; font-family: inherit; margin-top: 8px; max-height: 280px; overflow-y: auto; border: 1px solid #e2e8f0; padding: 10px; border-radius: 4px; background: #fafafa; }
                .detail-link { display: inline-block; margin-top: 12px; color: #2563eb; text-decoration: none; font-size: 12px; font-weight: 600; }
                .detail-link:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <header class="navbar">
                <div class="navbar-brand">
                    <span>JOB APPLICATION ORCHESTRATOR</span>
                    <span class="navbar-tag">NAVBAR</span>
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
                </div>

                <!-- Column 2: Job Feed & Controls (1fr) -->
                <div class="panel" style="padding: 16px;">
                    <div class="controls-bar">
                        <div class="filter-group">
                            <button class="btn-filter active" onclick="setFilter('ALL', this)">ALL</button>
                            <button class="btn-filter" onclick="setFilter('RECOMMENDED', this)">RECOMMENDED</button>
                            <button class="btn-filter" onclick="setFilter('CONDITIONAL', this)">CONDITIONAL</button>
                            <button class="btn-filter" onclick="setFilter('REJECTED', this)">REJECTED</button>
                        </div>
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <span id="job-count" style="font-size: 12px; color: #64748b; font-weight: 600;">0 Jobs</span>
                            <button id="btn-discover" class="btn-action" onclick="discoverJobs()">
                                <span>Discover Jobs</span>
                            </button>
                        </div>
                    </div>
                    <div class="job-list" id="job-list">
                        <div style="text-align: center; color: #94a3b8; padding: 40px;">Loading job opportunities...</div>
                    </div>
                </div>

                <!-- Column 3: Job Detail Panel (320px) -->
                <div class="panel">
                    <div class="panel-header">
                        <span>Job Details</span>
                    </div>
                    <div class="panel-content" id="detail-panel">
                        <div class="detail-placeholder">Select a job card to view full details</div>
                    </div>
                </div>
            </div>

            <script>
                let allJobs = [];
                let activeFilter = 'ALL';
                let selectedJobId = null;

                document.addEventListener('DOMContentLoaded', () => {
                    loadProfile();
                    loadJobs();
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
                        allJobs = await res.json();
                        renderJobs();
                    } catch (err) {
                        listEl.innerHTML = `<div style="text-align: center; color: #dc2626; padding: 40px;">Error loading jobs: ${escapeHtml(err.message)}</div>`;
                    }
                }

                function renderJobs() {
                    const listEl = document.getElementById('job-list');
                    const countEl = document.getElementById('job-count');

                    const filtered = allJobs.filter(job => {
                        const st = (job.status || 'DISCOVERED').toUpperCase();
                        if (activeFilter === 'ALL') return true;
                        return st === activeFilter;
                    });

                    countEl.textContent = `${filtered.length} Jobs`;

                    if (filtered.length === 0) {
                        listEl.innerHTML = '<div style="text-align: center; color: #94a3b8; padding: 40px;">No jobs found matching filter.</div>';
                        return;
                    }

                    listEl.innerHTML = filtered.map(job => {
                        const st = (job.status || 'DISCOVERED').toUpperCase();
                        let badgeClass = 'badge-discovered';
                        let badgeSymbol = '⚪';
                        if (st === 'RECOMMENDED') { badgeClass = 'badge-recommended'; badgeSymbol = '🟢'; }
                        else if (st === 'CONDITIONAL') { badgeClass = 'badge-conditional'; badgeSymbol = '🟡'; }
                        else if (st === 'REJECTED') { badgeClass = 'badge-rejected'; badgeSymbol = '🔴'; }

                        const isSelected = job.id === selectedJobId;

                        return `
                            <div class="job-card ${isSelected ? 'selected' : ''}" onclick="selectJob('${escapeHtml(job.id)}')">
                                <div class="job-card-header">
                                    <span class="job-card-title">${escapeHtml(job.title || 'Untitled')}</span>
                                    <span class="badge ${badgeClass}">${badgeSymbol} ${escapeHtml(st)}</span>
                                </div>
                                <div class="job-card-company">${escapeHtml(job.company || 'Unknown')}</div>
                                <div class="job-card-meta">
                                    <span>📍 ${escapeHtml(job.location || 'Remote')}</span>
                                    <span>💼 ${escapeHtml(job.seniorityLevel || 'N/A')}</span>
                                    <span>🌐 ${escapeHtml(job.workMode || 'N/A')}</span>
                                </div>
                            </div>
                        `;
                    }).join('');
                }

                function selectJob(id) {
                    selectedJobId = id;
                    renderJobs();
                    const job = allJobs.find(j => j.id === id);
                    const detailEl = document.getElementById('detail-panel');
                    if (!job) {
                        detailEl.innerHTML = '<div class="detail-placeholder">Select a job card to view full details</div>';
                        return;
                    }

                    const reqSkills = (job.requiredSkills || []).map(s => typeof s === 'string' ? s : s.name).join(', ') || 'None listed';
                    const optSkills = (job.optionalSkills || []).map(s => typeof s === 'string' ? s : s.name).join(', ') || 'None listed';
                    const st = (job.status || 'DISCOVERED').toUpperCase();
                    const rawDesc = job.description || 'No description available.';
                    const cleanDesc = escapeHtml(rawDesc).replace(/\\n/g, '<br>');

                    detailEl.innerHTML = `
                        <div class="detail-title">${escapeHtml(job.title || 'Untitled')}</div>
                        <div class="detail-company">${escapeHtml(job.company || 'Unknown')}</div>

                        <table class="detail-meta-table">
                            <tr><td class="label">Status</td><td class="val">${escapeHtml(st)}</td></tr>
                            <tr><td class="label">Seniority</td><td class="val">${escapeHtml(job.seniorityLevel || 'N/A')}</td></tr>
                            <tr><td class="label">Work Mode</td><td class="val">${escapeHtml(job.workMode || 'N/A')}</td></tr>
                            <tr><td class="label">Location</td><td class="val">${escapeHtml(job.location || 'N/A')}</td></tr>
                            <tr><td class="label">Req. Skills</td><td class="val">${escapeHtml(reqSkills)}</td></tr>
                            <tr><td class="label">Opt. Skills</td><td class="val">${escapeHtml(optSkills)}</td></tr>
                        </table>

                        <div class="section-label">Job Description</div>
                        <div class="detail-desc">${cleanDesc}</div>

                        ${job.sourceUrl ? `<a class="detail-link" href="${escapeHtml(job.sourceUrl)}" target="_blank">View Original Post ↗</a>` : ''}
                    `;
                }

                function setFilter(filter, btn) {
                    activeFilter = filter;
                    document.querySelectorAll('.btn-filter').forEach(b => b.classList.remove('active'));
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
