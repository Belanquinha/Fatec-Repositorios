import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadProjeto } from './upload-projeto';

describe('UploadProjeto', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
