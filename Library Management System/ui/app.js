/**
 * Library Management System — Frontend Logic (app.js)
 * =====================================================
 * This file simulates the backend operations in-browser using the
 * same dataset from database.sql. All element IDs are unique and
 * Selenium-friendly.
 *
 * Data mirrors database.sql:
 *   - users table (user_id, username, password, role)
 *   - books table (bid, title, author, quantity)
 *   - borrow table (borrow_id, book_id, user_id, borrow_date, return_date)
 */

'use strict';

/* =====================================================================
   IN-MEMORY DATABASE (mirrors database.sql)
   ===================================================================== */

const DB = {
  users: [
    { user_id: 12345678, username: 'Rohit Sharma',    password: 'rohit@123',    role: 'admin' },
    { user_id: 23456781, username: 'Anita Verma',     password: 'anita@2024',   role: 'admin' },
    { user_id: 34567812, username: 'Karthik Reddy',   password: 'karthik#12',   role: 'user'  },
    { user_id: 45678123, username: 'Meena Kumari',    password: 'meena_2001',   role: 'user'  },
    { user_id: 56781234, username: 'Sathvik Rao',     password: 'sathvik@777',  role: 'user'  },
    { user_id: 67812345, username: 'Divya Shah',      password: 'divya2020',    role: 'user'  },
    { user_id: 78123456, username: 'Rahul Patil',     password: 'rahul_pass',   role: 'user'  },
    { user_id: 81234567, username: 'Sneha Iyer',      password: 'sneha@001',    role: 'user'  },
    { user_id: 83947261, username: 'Pooja Singh',     password: 'pooja@06',     role: 'user'  },
    { user_id: 92740315, username: 'Harsha Vardhan',  password: 'harsha#09',    role: 'user'  },
  ],

  books: [
    { bid: 12001, title: "Programming in java",                      author: "Sanjay Gupta",       quantity: 7  },
    { bid: 12002, title: "To Kill a Mockingbird",                    author: "Harper Lee",          quantity: 5  },
    { bid: 12003, title: "1984",                                     author: "George Orwell",       quantity: 6  },
    { bid: 12004, title: "The Great Gatsby",                         author: "F. Scott Fitzgerald", quantity: 4  },
    { bid: 12005, title: "The Alchemist",                            author: "Paulo Coelho",        quantity: 10 },
    { bid: 12006, title: "Pride and Prejudice",                      author: "Jane Austen",         quantity: 6  },
    { bid: 12007, title: "The Catcher in the Rye",                   author: "J.D. Salinger",       quantity: 2  },
    { bid: 12008, title: "The Hobbit",                               author: "J.R.R. Tolkien",      quantity: 8  },
    { bid: 12009, title: "Harry Potter and the Sorcerer's Stone",    author: "J.K. Rowling",        quantity: 12 },
    { bid: 12010, title: "The Da Vinci Code",                        author: "Dan Brown",           quantity: 4  },
    { bid: 12011, title: "A Brief History of Time",                  author: "Stephen Hawking",     quantity: 2  },
    { bid: 12012, title: "Think and Grow Rich",                      author: "Napoleon Hill",       quantity: 9  },
    { bid: 12013, title: "Rich Dad Poor Dad",                        author: "Robert Kiyosaki",     quantity: 0  },
    { bid: 12014, title: "The Power of Habit",                       author: "Charles Duhigg",      quantity: 5  },
    { bid: 12015, title: "Clean Code",                               author: "Robert C. Martin",    quantity: 4  },
  ],

  borrow: [
    { borrow_id: 101, book_id: 12006, user_id: 45678123, borrow_date: '2025-12-02', return_date: '2025-12-02' },
    { borrow_id: 102, book_id: 12014, user_id: 34567812, borrow_date: '2025-12-02', return_date: null         },
    { borrow_id: 103, book_id: 12010, user_id: 67812345, borrow_date: '2025-12-02', return_date: null         },
    { borrow_id: 104, book_id: 12007, user_id: 78123456, borrow_date: '2025-12-02', return_date: null         },
    { borrow_id: 105, book_id: 12003, user_id: 81234567, borrow_date: '2025-12-02', return_date: null         },
  ],

  nextBorrowId: 106,
};

/* =====================================================================
   SESSION STATE
   ===================================================================== */
let currentUser = null; // { user_id, username, role }

/* =====================================================================
   HELPERS
   ===================================================================== */

/** Show/hide a DOM element by toggling the 'hidden' class. */
function show(el) { el && el.classList.remove('hidden'); }
function hide(el) { el && el.classList.add('hidden'); }
function toggle(el, visible) { visible ? show(el) : hide(el); }

/** Get today's date as YYYY-MM-DD */
function today() { return new Date().toISOString().split('T')[0]; }

/** Show an inline message inside a form */
function showMsg(id, message, type = 'success') {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = message;
  el.className = `alert alert-${type}`;
  show(el);
  setTimeout(() => hide(el), 4000);
}

/** Show a toast popup */
function showToast(message, type = 'info') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast toast-${type} show`;
  setTimeout(() => { toast.className = 'toast hidden'; }, 3200);
}

/** Render a status badge based on quantity */
function statusBadge(qty) {
  if (qty === 0) return `<span class="badge badge-danger">Out of Stock</span>`;
  if (qty <= 2)  return `<span class="badge badge-warn">Low Stock</span>`;
  return `<span class="badge badge-success">Available</span>`;
}

/** Build a table row for a book record */
function bookRow(b) {
  return `<tr id="book-row-${b.bid}">
    <td>${b.bid}</td>
    <td>${b.title}</td>
    <td>${b.author}</td>
    <td>${b.quantity}</td>
    <td>${statusBadge(b.quantity)}</td>
  </tr>`;
}

/** Populate a tbody with books array */
function renderBooks(tbodyId, emptyId, books) {
  const tbody = document.getElementById(tbodyId);
  const empty = document.getElementById(emptyId);
  if (!tbody) return;
  if (books.length === 0) {
    tbody.innerHTML = '';
    if (empty) show(empty);
    return;
  }
  if (empty) hide(empty);
  tbody.innerHTML = books.map(bookRow).join('');
}

/* =====================================================================
   PAGE NAVIGATION
   ===================================================================== */

function showPage(pageId) {
  document.querySelectorAll('.page').forEach(p => {
    p.classList.remove('active');
    p.classList.add('hidden');
  });
  const page = document.getElementById(pageId);
  if (page) {
    page.classList.remove('hidden');
    page.classList.add('active');
  }
}

function showSection(sectionId, navParentId) {
  const parent = document.getElementById(navParentId);
  if (!parent) return;
  parent.querySelectorAll('.content-section').forEach(s => {
    s.classList.remove('active');
    s.classList.add('hidden');
  });
  const section = document.getElementById(sectionId);
  if (section) {
    section.classList.remove('hidden');
    section.classList.add('active');
  }
}

function setActiveNav(clickedLink, navId) {
  document.querySelectorAll(`#${navId} .nav-item`).forEach(a => a.classList.remove('active'));
  clickedLink.classList.add('active');
}

function updatePageTitle(titleId, text) {
  const el = document.getElementById(titleId);
  if (el) el.textContent = text;
}

/* =====================================================================
   LOGIN
   ===================================================================== */

document.getElementById('login-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value.trim();
  const errorEl = document.getElementById('login-error');

  hide(errorEl);

  const user = DB.users.find(u => u.username === username && u.password === password);

  if (!user) {
    show(errorEl);
    document.getElementById('login-username').focus();
    return;
  }

  currentUser = user;

  if (user.role === 'admin') {
    document.getElementById('admin-welcome-msg').textContent = 'Admin: ' + user.username;
    showPage('admin-page');
    showSection('admin-dashboard-section', 'admin-main');
    updatePageTitle('admin-page-title', 'Dashboard');
  } else {
    document.getElementById('user-welcome-msg').textContent = user.username;
    showPage('user-page');
    showSection('user-dashboard-section', 'user-main');
    updatePageTitle('user-page-title', 'Dashboard');
  }
  this.reset();
});

/* Toggle password visibility */
document.getElementById('toggle-password').addEventListener('click', function () {
  const inp = document.getElementById('login-password');
  inp.type = inp.type === 'password' ? 'text' : 'password';
});

/* =====================================================================
   LOGOUT
   ===================================================================== */

document.getElementById('admin-logout-btn').addEventListener('click', () => {
  currentUser = null;
  showPage('login-page');
});

document.getElementById('user-logout-btn').addEventListener('click', () => {
  currentUser = null;
  showPage('login-page');
});

/* =====================================================================
   ADMIN SIDEBAR NAVIGATION
   ===================================================================== */

document.getElementById('admin-nav').addEventListener('click', function (e) {
  e.preventDefault();
  const link = e.target.closest('.nav-item');
  if (!link) return;
  const sectionId = link.dataset.section;
  setActiveNav(link, 'admin-nav');
  showSection(sectionId, 'admin-main');
  const label = link.textContent.trim();
  updatePageTitle('admin-page-title', label);

  // Auto-load data sections when navigated to
  if (sectionId === 'admin-view-books-section') loadAdminBooks();
  if (sectionId === 'admin-zero-qty-section')   loadZeroQtyBooks();
  if (sectionId === 'admin-borrowed-section')   loadBorrowedBooks();
});

/* =====================================================================
   USER SIDEBAR NAVIGATION
   ===================================================================== */

document.getElementById('user-nav').addEventListener('click', function (e) {
  e.preventDefault();
  const link = e.target.closest('.nav-item');
  if (!link) return;
  const sectionId = link.dataset.section;
  setActiveNav(link, 'user-nav');
  showSection(sectionId, 'user-main');
  const label = link.textContent.trim();
  updatePageTitle('user-page-title', label);

  if (sectionId === 'user-view-books-section') loadUserBooks();
});

/* =====================================================================
   SEARCH TABS
   ===================================================================== */

function initTabs(tabsId, panelScope) {
  const container = document.getElementById(tabsId);
  if (!container) return;
  container.addEventListener('click', function (e) {
    const btn = e.target.closest('.tab-btn');
    if (!btn) return;
    container.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    const scope = btn.closest('.card') || document;
    scope.querySelectorAll('.tab-panel').forEach(p => {
      p.classList.remove('active');
      p.classList.add('hidden');
    });
    const panel = document.getElementById(btn.dataset.tab);
    if (panel) { panel.classList.remove('hidden'); panel.classList.add('active'); }
  });
}

initTabs('admin-search-tabs');
initTabs('user-search-tabs');

/* =====================================================================
   ADMIN: LOAD ALL BOOKS
   ===================================================================== */

function loadAdminBooks() {
  renderBooks('admin-books-tbody', 'admin-books-empty', DB.books);
}

document.getElementById('admin-refresh-books-btn').addEventListener('click', loadAdminBooks);

/* =====================================================================
   ADMIN: ADD BOOK
   ===================================================================== */

document.getElementById('add-book-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const id    = parseInt(document.getElementById('add-book-id').value);
  const title = document.getElementById('add-book-title').value.trim();
  const author= document.getElementById('add-book-author').value.trim();
  const qty   = parseInt(document.getElementById('add-book-quantity').value);

  if (!id || !title || !author || isNaN(qty) || qty < 0) {
    showMsg('add-book-msg', 'Please fill all fields correctly.', 'error');
    return;
  }

  if (DB.books.find(b => b.bid === id)) {
    showMsg('add-book-msg', 'A book with this ID already exists.', 'error');
    return;
  }
  if (DB.books.find(b => b.title.toLowerCase() === title.toLowerCase())) {
    showMsg('add-book-msg', 'A book with this title already exists.', 'error');
    return;
  }

  DB.books.push({ bid: id, title, author, quantity: qty });
  showMsg('add-book-msg', 'Book Added Successfully!', 'success');
  showToast('Book "' + title + '" added!', 'success');
  this.reset();
});

/* =====================================================================
   ADMIN: DELETE BOOK
   ===================================================================== */

document.getElementById('delete-book-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const id = parseInt(document.getElementById('delete-book-id').value);

  const idx = DB.books.findIndex(b => b.bid === id);
  if (idx === -1) {
    showMsg('delete-book-msg', 'Invalid Book ID. Book not found.', 'error');
    return;
  }

  const removed = DB.books.splice(idx, 1)[0];
  showMsg('delete-book-msg', `Book "${removed.title}" deleted successfully!`, 'success');
  showToast('Book deleted.', 'success');
  this.reset();
});

/* =====================================================================
   ADMIN: UPDATE QUANTITY
   ===================================================================== */

document.getElementById('update-qty-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const id  = parseInt(document.getElementById('update-qty-book-id').value);
  const qty = parseInt(document.getElementById('update-qty-value').value);

  const book = DB.books.find(b => b.bid === id);
  if (!book) {
    showMsg('update-qty-msg', 'Invalid Book ID. Book not found.', 'error');
    return;
  }
  if (isNaN(qty) || qty < 0) {
    showMsg('update-qty-msg', 'Please enter a valid quantity (0 or more).', 'error');
    return;
  }

  book.quantity = qty;
  showMsg('update-qty-msg', `Quantity updated to ${qty} for "${book.title}".`, 'success');
  showToast('Quantity updated!', 'success');
  this.reset();
});

/* =====================================================================
   ADMIN: SEARCH BOOKS
   ===================================================================== */

function renderSearchResults(results, tbodyId, emptyId, resultsContainerId) {
  const container = document.getElementById(resultsContainerId);
  show(container);
  renderBooks(tbodyId, emptyId, results);
}

// Search by ID
document.getElementById('admin-search-id-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const id = parseInt(document.getElementById('admin-search-book-id').value);
  const results = DB.books.filter(b => b.bid === id);
  renderSearchResults(results, 'admin-search-tbody', 'admin-search-empty', 'admin-search-results');
});

// Search by Title
document.getElementById('admin-search-title-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const title = document.getElementById('admin-search-book-title').value.trim().toLowerCase();
  const results = DB.books.filter(b => b.title.toLowerCase().includes(title));
  renderSearchResults(results, 'admin-search-tbody', 'admin-search-empty', 'admin-search-results');
});

// Search by Author
document.getElementById('admin-search-author-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const author = document.getElementById('admin-search-book-author').value.trim().toLowerCase();
  const results = DB.books.filter(b => b.author.toLowerCase().includes(author));
  renderSearchResults(results, 'admin-search-tbody', 'admin-search-empty', 'admin-search-results');
});

/* =====================================================================
   ADMIN: ZERO QUANTITY BOOKS
   ===================================================================== */

function loadZeroQtyBooks() {
  const zeroBooksArr = DB.books.filter(b => b.quantity === 0);
  const tbody = document.getElementById('admin-zero-tbody');
  const empty = document.getElementById('admin-zero-empty');

  if (zeroBooksArr.length === 0) {
    tbody.innerHTML = '';
    show(empty);
    return;
  }
  hide(empty);
  tbody.innerHTML = zeroBooksArr.map(b => `
    <tr id="zero-row-${b.bid}">
      <td>${b.bid}</td>
      <td>${b.title}</td>
      <td>${b.author}</td>
      <td><span class="badge badge-danger">0</span></td>
    </tr>
  `).join('');
}

document.getElementById('admin-refresh-zero-btn').addEventListener('click', loadZeroQtyBooks);

/* =====================================================================
   ADMIN: BORROWED BOOKS
   ===================================================================== */

function loadBorrowedBooks() {
  const tbody = document.getElementById('admin-borrowed-tbody');
  const empty = document.getElementById('admin-borrowed-empty');

  if (DB.borrow.length === 0) {
    tbody.innerHTML = '';
    show(empty);
    return;
  }
  hide(empty);

  tbody.innerHTML = DB.borrow.map(rec => {
    const user = DB.users.find(u => u.user_id === rec.user_id);
    const book = DB.books.find(b => b.bid === rec.book_id);
    const returnDateDisplay = rec.return_date
      ? `<span class="badge badge-success">${rec.return_date}</span>`
      : `<span class="badge badge-warn">Not Returned</span>`;
    return `<tr id="borrow-row-${rec.borrow_id}">
      <td>${rec.borrow_id}</td>
      <td>${user ? user.username : 'Unknown'}</td>
      <td>${book ? book.title : 'Unknown'}</td>
      <td>${book ? book.author : '-'}</td>
      <td>${rec.borrow_date}</td>
      <td>${returnDateDisplay}</td>
    </tr>`;
  }).join('');
}

document.getElementById('admin-refresh-borrowed-btn').addEventListener('click', loadBorrowedBooks);

/* =====================================================================
   USER: LOAD BOOKS
   ===================================================================== */

function loadUserBooks() {
  renderBooks('user-books-tbody', 'user-books-empty', DB.books);
}

document.getElementById('user-refresh-books-btn').addEventListener('click', loadUserBooks);

/* =====================================================================
   USER: BORROW BOOK
   ===================================================================== */

document.getElementById('borrow-book-form').addEventListener('submit', function (e) {
  e.preventDefault();
  if (!currentUser) return;

  const bookId = parseInt(document.getElementById('borrow-book-id').value);
  const book = DB.books.find(b => b.bid === bookId);

  if (!book) {
    showMsg('borrow-book-msg', 'Invalid Book ID!', 'error');
    return;
  }
  if (book.quantity <= 0) {
    showMsg('borrow-book-msg', 'Book Not Available! Out of stock.', 'error');
    return;
  }

  // Check if user already has this book borrowed
  const alreadyBorrowed = DB.borrow.find(
    r => r.user_id === currentUser.user_id && r.book_id === bookId && r.return_date === null
  );
  if (alreadyBorrowed) {
    showMsg('borrow-book-msg', 'You have already borrowed this book!', 'warn');
    return;
  }

  book.quantity -= 1;
  DB.borrow.push({
    borrow_id: DB.nextBorrowId++,
    book_id: bookId,
    user_id: currentUser.user_id,
    borrow_date: today(),
    return_date: null,
  });

  showMsg('borrow-book-msg', `Book "${book.title}" borrowed successfully!`, 'success');
  showToast('Book borrowed!', 'success');
  this.reset();
});

/* =====================================================================
   USER: RETURN BOOK
   ===================================================================== */

document.getElementById('return-book-form').addEventListener('submit', function (e) {
  e.preventDefault();
  if (!currentUser) return;

  const bookId = parseInt(document.getElementById('return-book-id').value);
  const borrowRecord = DB.borrow.find(
    r => r.user_id === currentUser.user_id && r.book_id === bookId && r.return_date === null
  );

  if (!borrowRecord) {
    showMsg('return-book-msg', 'You have not borrowed this book!', 'error');
    return;
  }

  const book = DB.books.find(b => b.bid === bookId);
  if (book) book.quantity += 1;
  borrowRecord.return_date = today();

  showMsg('return-book-msg', `Book "${book ? book.title : bookId}" returned successfully!`, 'success');
  showToast('Book returned!', 'success');
  this.reset();
});

/* =====================================================================
   USER: SEARCH BOOKS
   ===================================================================== */

// Search by Title
document.getElementById('user-search-title-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const title = document.getElementById('user-search-book-title').value.trim().toLowerCase();
  const results = DB.books.filter(b => b.title.toLowerCase().includes(title));
  renderSearchResults(results, 'user-search-tbody', 'user-search-empty', 'user-search-results');
});

// Search by Author
document.getElementById('user-search-author-form').addEventListener('submit', function (e) {
  e.preventDefault();
  const author = document.getElementById('user-search-book-author').value.trim().toLowerCase();
  const results = DB.books.filter(b => b.author.toLowerCase().includes(author));
  renderSearchResults(results, 'user-search-tbody', 'user-search-empty', 'user-search-results');
});
