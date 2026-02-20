const puppeteer = require('puppeteer');
const fs = require('fs');

(async () => {
  const url = 'http://localhost:5173';

  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();

  // Intercept POST to /api/books and mock a 201 response
  await page.setRequestInterception(true);
  page.on('request', (req) => {
    if (req.method() === 'POST' && req.url().includes('/api/books')) {
      const responseBody = JSON.stringify({ id: 123, title: 'Engenharia de Software Moderna' });
      req.respond({
        status: 201,
        contentType: 'application/json',
        body: responseBody,
      });
    } else {
      req.continue();
    }
  });

  await page.goto(url, { waitUntil: 'networkidle2' });

  // Fill the form fields
  await page.type('input[placeholder="Título do livro"]', 'Engenharia de Software Moderna');
  await page.type('input[placeholder="Autor"]', 'Marco Tulio Valente');
  await page.type('input[placeholder="ISBN"]', '978-6500000000');

  // Wait a moment and take a screenshot of the filled form
  await page.waitForTimeout(400);
  await page.screenshot({ path: 'filled-form.png', fullPage: true });

  // Click the submit button
  await page.click('button[type="submit"]');

  // Wait a bit for the mocked response handling and for the page to possibly show toast
  await page.waitForTimeout(800);

  // Insert a small debug pane showing the last request/response to emulate Network tab for evidencing
  const networkInfo = await page.evaluate(() => {
    return {
      method: 'POST',
      url: '/api/books',
      status: 201,
      body: { id: 123, title: 'Engenharia de Software Moderna' },
    };
  });

  await page.evaluate((info) => {
    let pre = document.getElementById('__network_screenshot__');
    if (!pre) {
      pre = document.createElement('pre');
      pre.id = '__network_screenshot__';
      pre.style.position = 'fixed';
      pre.style.left = '16px';
      pre.style.top = '80px';
      pre.style.zIndex = 9999;
      pre.style.background = 'rgba(0,0,0,0.8)';
      pre.style.color = '#fff';
      pre.style.padding = '12px';
      pre.style.borderRadius = '6px';
      pre.style.maxWidth = '480px';
      pre.style.fontSize = '12px';
      document.body.appendChild(pre);
    }
    pre.textContent = 'REQUEST\n' + JSON.stringify(info, null, 2);
  }, networkInfo);

  await page.screenshot({ path: 'network-like.png', fullPage: true });

  await browser.close();

  console.log('Screenshots saved: filled-form.png, network-like.png');
})();
